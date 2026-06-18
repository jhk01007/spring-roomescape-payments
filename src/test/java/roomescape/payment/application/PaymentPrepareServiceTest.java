package roomescape.payment.application;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.common.exception.DomainException;
import roomescape.payment.application.port.in.PaymentPrepareUseCase;
import roomescape.payment.application.port.in.dto.PaymentPrepareCommand;
import roomescape.payment.application.port.in.dto.PaymentPrepareResult;
import roomescape.payment.application.port.out.PaymentSessionRepository;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;
import roomescape.reservation.domain.ReservationStatus;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.test_config.clock.MutableClock;
import roomescape.test_config.fixture.SQLFixtureGenerator;
import roomescape.test_config.integration.db.service.ServiceTest;
import roomescape.theme.domain.Theme;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_EXPIRED;
import static roomescape.reservation.domain.ReservationStatus.CANCELED;
import static roomescape.reservation.domain.ReservationStatus.PENDING;

@ServiceTest
class PaymentPrepareServiceTest {

    private static final Long AMOUNT = 43_000L;

    @Autowired
    PaymentPrepareUseCase paymentPrepareUseCase;

    @Autowired
    PaymentSessionRepository paymentSessionRepository;

    @Autowired
    ReservationRepository reservationRepository;

    @Autowired
    SQLFixtureGenerator fixtureGenerator;

    @Autowired
    MutableClock clock;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("결제 준비 시 결제 대기 예약의 주문 정보를 저장한다.")
    void prepare_success() {
        // given
        LocalDateTime now = LocalDateTime.of(2025, 5, 1, 12, 0);
        clock.setFixed(now);

        Reservation reservation = insertReservation(PENDING, now, now.plusMinutes(10));

        // when
        PaymentPrepareResult result = paymentPrepareUseCase.prepare(new PaymentPrepareCommand(reservation.getId()));

        // then
        assertThat(result.orderId()).startsWith("order-");
        assertThat(result.reservationId()).isEqualTo(reservation.getId());
        assertThat(result.amount()).isEqualTo(AMOUNT);
        assertThat(result.orderName()).isEqualTo("레벨2 탈출");
        assertThat(paymentSessionRepository.findById(result.orderId())).isPresent();
    }

    @Test
    @DisplayName("만료된 결제 대기 예약은 결제 준비를 할 수 없고 취소된다.")
    void prepare_fail_expiredReservation() {
        // given
        LocalDateTime now = LocalDateTime.of(2025, 5, 1, 12, 0);
        clock.setFixed(now);

        Reservation reservation = insertReservation(PENDING, now.minusMinutes(11), now.minusMinutes(1));

        // when, then
        assertThatThrownBy(() -> paymentPrepareUseCase.prepare(new PaymentPrepareCommand(reservation.getId())))
                .isInstanceOf(DomainException.class)
                .hasMessage(PAYMENT_EXPIRED.message());

        entityManager.flush();
        entityManager.clear();

        assertThat(reservationRepository.findById(reservation.getId()).get().getReservationStatus()).isEqualTo(CANCELED);
    }

    private Reservation insertReservation(
            ReservationStatus reservationStatus,
            LocalDateTime lastModifiedAt,
            LocalDateTime paymentExpiresAt
    ) {
        ReservationTime time = fixtureGenerator.insertReservationTime(LocalTime.of(10, 0));
        Theme theme = fixtureGenerator.insertTheme(
                "레벨2 탈출",
                "우테코 레벨2를 탈출하는 내용입니다.",
                "https://example.com/theme.png",
                AMOUNT
        );
        return fixtureGenerator.insertReservation(
                "브라운",
                LocalDate.of(2025, 5, 2),
                time,
                theme,
                reservationStatus,
                lastModifiedAt,
                paymentExpiresAt
        );
    }
}

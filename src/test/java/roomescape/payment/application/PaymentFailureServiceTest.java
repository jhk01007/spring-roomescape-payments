package roomescape.payment.application;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.common.exception.DomainException;
import roomescape.payment.application.port.in.dto.PaymentFailureResult;
import roomescape.payment.application.port.out.PaymentSessionRepository;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.test_config.fixture.SQLFixtureGenerator;
import roomescape.test_config.integration.db.service.ServiceTest;
import roomescape.theme.domain.Theme;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_FAILURE_NOT_ALLOWED;
import static roomescape.reservation.domain.Status.CANCELED;
import static roomescape.reservation.domain.Status.CONFIRMED;
import static roomescape.reservation.domain.Status.PENDING;

@ServiceTest
class PaymentFailureServiceTest {

    private static final Long AMOUNT = 43_000L;

    @Autowired
    PaymentFailureService paymentFailureService;

    @Autowired
    PaymentSessionRepository paymentSessionRepository;

    @Autowired
    ReservationRepository reservationRepository;

    @Autowired
    SQLFixtureGenerator fixtureGenerator;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("결제 실패를 처리하면 결제 대기 예약이 취소된다.")
    void fail_success() {
        // given
        Reservation reservation = insertReservation(PENDING);
        paymentSessionRepository.save("order-1", reservation.getId(), AMOUNT);

        // when
        PaymentFailureResult result = paymentFailureService.fail("order-1");

        // then
        entityManager.flush();
        entityManager.clear();

        Reservation updatedReservation = reservationRepository.findById(reservation.getId()).get();
        assertThat(updatedReservation.getStatus()).isEqualTo(CANCELED);
        assertThat(result.orderId()).isEqualTo("order-1");
        assertThat(result.reservationId()).isEqualTo(reservation.getId());
        assertThat(result.reservationStatus()).isEqualTo(CANCELED);
    }

    @Test
    @DisplayName("이미 확정된 예약은 결제 실패 처리로 취소할 수 없다.")
    void fail_fail_confirmedReservation() {
        // given
        Reservation reservation = insertReservation(CONFIRMED);
        paymentSessionRepository.save("order-2", reservation.getId(), AMOUNT);

        // when, then
        assertThatThrownBy(() -> paymentFailureService.fail("order-2"))
                .isInstanceOf(DomainException.class)
                .hasMessage(PAYMENT_FAILURE_NOT_ALLOWED.message());
    }

    private Reservation insertReservation(roomescape.reservation.domain.Status status) {
        ReservationTime time = fixtureGenerator.insertReservationTime(LocalTime.of(10, 0));
        Theme theme = fixtureGenerator.insertTheme(
                "레벨2 탈출",
                "우테코 레벨2를 탈출하는 내용입니다.",
                "https://example.com/theme.png",
                AMOUNT
        );
        return fixtureGenerator.insertReservation("브라운", LocalDate.of(2025, 5, 2), time, theme, status);
    }
}

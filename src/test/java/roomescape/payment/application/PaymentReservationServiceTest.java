package roomescape.payment.application;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.payment.application.port.in.PaymentReservationUseCase;
import roomescape.payment.application.port.in.dto.PaymentReservationCommand;
import roomescape.payment.application.port.in.dto.PaymentReservationResult;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.test_config.clock.MutableClock;
import roomescape.test_config.fixture.SQLFixtureGenerator;
import roomescape.test_config.integration.db.service.ServiceTest;
import roomescape.theme.domain.Theme;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static roomescape.reservation.domain.ReservationStatus.PENDING;

@ServiceTest
class PaymentReservationServiceTest {

    @Autowired
    PaymentReservationUseCase paymentReservationUseCase;

    @Autowired
    ReservationRepository reservationRepository;

    @Autowired
    SQLFixtureGenerator fixtureGenerator;

    @Autowired
    MutableClock clock;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("결제 예약을 생성하면 결제 만료 시각을 10분 뒤로 저장한다.")
    void create_success_paymentExpiresAtAfter10Minutes() {
        // given
        LocalDateTime now = LocalDateTime.of(2025, 5, 1, 12, 0);
        clock.setFixed(now);

        ReservationTime time = fixtureGenerator.insertReservationTime(LocalTime.of(10, 0));
        Theme theme = fixtureGenerator.insertTheme(
                "레벨2 탈출",
                "우테코 레벨2를 탈출하는 내용입니다.",
                "https://example.com/theme.png",
                40_000L
        );
        PaymentReservationCommand command = new PaymentReservationCommand(
                "브라운",
                LocalDate.of(2025, 5, 2),
                time.getId(),
                theme.getId()
        );

        // when
        PaymentReservationResult result = paymentReservationUseCase.create(command);

        // then
        entityManager.flush();
        entityManager.clear();

        Reservation reservation = reservationRepository.findById(result.reservationId()).get();
        assertThat(reservation.getReservationStatus()).isEqualTo(PENDING);
        assertThat(reservation.getLastModifiedAt()).isEqualTo(now);
        assertThat(reservation.getPaymentExpiresAt()).isEqualTo(now.plusMinutes(10));
        assertThat(result.amount()).isEqualTo(theme.getPrice());
        assertThat(result.orderName()).isEqualTo(theme.getName());
    }
}

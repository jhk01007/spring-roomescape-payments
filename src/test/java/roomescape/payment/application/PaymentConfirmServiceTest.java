package roomescape.payment.application;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.common.exception.DomainException;
import roomescape.payment.application.port.out.PaymentSessionRepository;
import roomescape.payment.application.service.PaymentConfirmService;
import roomescape.reservation.domain.Reservation;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.test_config.fixture.SQLFixtureGenerator;
import roomescape.test_config.integration.db.service.ServiceTest;
import roomescape.theme.domain.Theme;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_ALREADY_PROCESSED;
import static roomescape.reservation.domain.ReservationStatus.PENDING;

@ServiceTest
class PaymentConfirmServiceTest {

    private static final Long AMOUNT = 43_000L;

    @Autowired
    PaymentConfirmService paymentConfirmService;

    @Autowired
    PaymentSessionRepository paymentSessionRepository;

    @Autowired
    SQLFixtureGenerator fixtureGenerator;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("이미 처리된 결제를 다시 승인하면 중복 처리 예외가 발생한다.")
    void confirm_fail_alreadyProcessedPayment() {
        // given
        Reservation reservation = insertPendingReservation();
        paymentSessionRepository.save("order-1", reservation.getId(), AMOUNT);
        paymentConfirmService.confirm("payment-key-1", "order-1", AMOUNT);
        entityManager.flush();
        entityManager.clear();

        // when, then
        assertThatThrownBy(() -> paymentConfirmService.confirm("payment-key-1", "order-1", AMOUNT))
                .isInstanceOf(DomainException.class)
                .hasMessage(PAYMENT_ALREADY_PROCESSED.message());
    }

    private Reservation insertPendingReservation() {
        ReservationTime time = fixtureGenerator.insertReservationTime(LocalTime.of(10, 0));
        Theme theme = fixtureGenerator.insertTheme(
                "레벨2 탈출",
                "우테코 레벨2를 탈출하는 내용입니다.",
                "https://example.com/theme.png",
                AMOUNT
        );
        return fixtureGenerator.insertReservation("브라운", LocalDate.of(2025, 5, 2), time, theme, PENDING);
    }
}

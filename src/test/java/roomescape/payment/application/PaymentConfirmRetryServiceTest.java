package roomescape.payment.application;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import roomescape.payment.adapter.out.client.TossPaymentException;
import roomescape.payment.application.port.out.PaymentGateway;
import roomescape.payment.application.port.out.PaymentSessionRepository;
import roomescape.payment.application.retry.TossPaymentRetryAspect;
import roomescape.payment.domain.PaymentConfirmation;
import roomescape.payment.domain.PaymentResult;
import roomescape.payment.domain.PaymentStatus;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.test_config.fixture.SQLFixtureGenerator;
import roomescape.test_config.integration.db.service.ServiceTest;
import roomescape.theme.domain.Theme;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static roomescape.reservation.domain.Status.CONFIRMED;
import static roomescape.reservation.domain.Status.PENDING;

@ServiceTest
@Import({
        PaymentConfirmRetryServiceTest.RetryGatewayTestConfig.class,
        TossPaymentRetryAspect.class
})
class PaymentConfirmRetryServiceTest {

    private static final Long AMOUNT = 43_000L;

    @Autowired
    PaymentConfirmService paymentConfirmService;

    @Autowired
    PaymentSessionRepository paymentSessionRepository;

    @Autowired
    ReservationRepository reservationRepository;

    @Autowired
    SQLFixtureGenerator fixtureGenerator;

    @Autowired
    RetryGatewayStub retryGatewayStub;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void resetGateway() {
        retryGatewayStub.reset();
    }

    @Test
    @DisplayName("Toss 승인 중 재시도 가능한 예외가 발생하면 재시도 후 결제를 완료한다.")
    void confirm_retry_success() {
        // given
        Reservation reservation = insertPendingReservation();
        paymentSessionRepository.save("order-1", reservation.getId(), AMOUNT);
        retryGatewayStub.failRetryableTimes(2);

        // when
        PaymentResult result = paymentConfirmService.confirm("payment-key-1", "order-1", AMOUNT);

        // then
        entityManager.flush();
        entityManager.clear();

        assertThat(retryGatewayStub.attempts()).isEqualTo(3);
        assertThat(result.paymentKey()).isEqualTo("payment-key-1");
        assertThat(reservationRepository.findById(reservation.getId()).get().getStatus()).isEqualTo(CONFIRMED);
    }

    @Test
    @DisplayName("재시도 가능한 Toss 승인 예외가 최대 재시도 횟수까지 발생하면 예외를 전파한다.")
    void confirm_retry_fail_exhausted() {
        // given
        Reservation reservation = insertPendingReservation();
        paymentSessionRepository.save("order-2", reservation.getId(), AMOUNT);
        retryGatewayStub.failRetryableTimes(3);

        // when, then
        assertThatThrownBy(() -> paymentConfirmService.confirm("payment-key-2", "order-2", AMOUNT))
                .isInstanceOf(TossPaymentException.Retryable.class);

        entityManager.flush();
        entityManager.clear();

        assertThat(retryGatewayStub.attempts()).isEqualTo(3);
        assertThat(reservationRepository.findById(reservation.getId()).get().getStatus()).isEqualTo(PENDING);
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

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class RetryGatewayTestConfig {

        @Bean
        @Primary
        RetryGatewayStub retryGatewayStub() {
            return new RetryGatewayStub();
        }
    }

    static class RetryGatewayStub implements PaymentGateway {

        private final AtomicInteger attempts = new AtomicInteger();
        private int retryableFailures;

        void failRetryableTimes(int retryableFailures) {
            this.retryableFailures = retryableFailures;
        }

        int attempts() {
            return attempts.get();
        }

        void reset() {
            attempts.set(0);
            retryableFailures = 0;
        }

        @Override
        public PaymentResult confirm(PaymentConfirmation confirmation) {
            int attempt = attempts.incrementAndGet();
            if (attempt <= retryableFailures) {
                throw new TossPaymentException.Retryable("토스 결제 승인 처리 중 일시적인 오류가 발생했습니다.");
            }
            return new PaymentResult(
                    confirmation.paymentKey(),
                    confirmation.orderId(),
                    PaymentStatus.DONE,
                    confirmation.amount(),
                    LocalDateTime.of(2025, 5, 1, 12, 1),
                    LocalDateTime.of(2025, 5, 1, 12, 0)
            );
        }
    }
}

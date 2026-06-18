package roomescape.payment.application;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.common.exception.DomainException;
import roomescape.payment.adapter.out.persistence.JpaTossPaymentRepository;
import roomescape.payment.application.port.in.PaymentCheckUseCase;
import roomescape.payment.application.port.in.dto.PaymentCheckResult;
import roomescape.payment.application.port.out.PaymentSessionRepository;
import roomescape.payment.application.service.PaymentCompleteService;
import roomescape.payment.domain.PaymentResult;
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
import static roomescape.payment.domain.PaymentStatus.DONE;
import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_EXPIRED;
import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_RESERVATION_NOT_PENDING;
import static roomescape.reservation.domain.ReservationStatus.CANCELED;
import static roomescape.reservation.domain.ReservationStatus.CONFIRMED;
import static roomescape.reservation.domain.ReservationStatus.PENDING;
import static roomescape.reservation.domain.ReservationStatus.REQUIRES_CHECK;

@ServiceTest
class PaymentCompleteServiceTest {

    private static final Long AMOUNT = 43_000L;
    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2025, 5, 1, 12, 0);
    private static final LocalDateTime APPROVED_AT = LocalDateTime.of(2025, 5, 1, 12, 1);

    @Autowired
    PaymentCompleteService paymentCompleteService;

    @Autowired
    PaymentCheckUseCase paymentCheckUseCase;

    @Autowired
    PaymentSessionRepository paymentSessionRepository;

    @Autowired
    JpaTossPaymentRepository jpaTossPaymentRepository;

    @Autowired
    ReservationRepository reservationRepository;

    @Autowired
    SQLFixtureGenerator fixtureGenerator;

    @Autowired
    MutableClock clock;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void resetClock() {
        clock.reset();
    }

    @Test
    @DisplayName("승인이 완료된 결제 정보를 저장하고 예약을 확정한다.")
    void complete_success() {
        // given
        Reservation reservation = insertReservation(PENDING);
        paymentSessionRepository.save("order-1", reservation.getId(), AMOUNT);

        // when
        paymentCompleteService.complete(paymentResult("payment-key-1", "order-1", AMOUNT));

        // then
        entityManager.flush();
        entityManager.clear();

        Reservation updatedReservation = reservationRepository.findById(reservation.getId()).get();
        assertThat(updatedReservation.getReservationStatus()).isEqualTo(CONFIRMED);

        assertThat(jpaTossPaymentRepository.findAll())
                .singleElement()
                .satisfies(payment -> {
                    assertThat(payment.getReservationId()).isEqualTo(reservation.getId());
                    assertThat(payment.getPaymentKey()).isEqualTo("payment-key-1");
                    assertThat(payment.getOrderId()).isEqualTo("order-1");
                    assertThat(payment.getAmount()).isEqualTo(AMOUNT);
                    assertThat(payment.getPaymentStatus()).isEqualTo(DONE);
                    assertThat(payment.getRequestedAt()).isEqualTo(REQUESTED_AT);
                    assertThat(payment.getApprovedAt()).isEqualTo(APPROVED_AT);
                });
    }

    @Test
    @DisplayName("결제 대기 상태가 아닌 예약은 결제 완료 처리할 수 없다.")
    void complete_fail_notPendingReservation() {
        // given
        Reservation reservation = insertReservation(CONFIRMED);
        paymentSessionRepository.save("order-2", reservation.getId(), AMOUNT);

        // when, then
        assertThatThrownBy(() -> paymentCompleteService.complete(paymentResult("payment-key-2", "order-2", AMOUNT)))
                .isInstanceOf(DomainException.class)
                .hasMessage(PAYMENT_RESERVATION_NOT_PENDING.message());
    }

    @Test
    @DisplayName("취소된 예약의 결제를 완료하려고 하면 만료된 결제 예외가 발생한다.")
    void complete_fail_expiredReservation() {
        // given
        Reservation reservation = insertReservation(CANCELED);
        paymentSessionRepository.save("order-3", reservation.getId(), AMOUNT);

        // when, then
        assertThatThrownBy(() -> paymentCompleteService.complete(paymentResult("payment-key-3", "order-3", AMOUNT)))
                .isInstanceOf(DomainException.class)
                .hasMessage(PAYMENT_EXPIRED.message());
    }

    @Test
    @DisplayName("결제 승인 결과를 알 수 없으면 결제 확인 필요 정보를 저장하고 예약을 확인 필요 상태로 변경한다.")
    void requireCheck_success() {
        // given
        Reservation reservation = insertReservation(PENDING);
        paymentSessionRepository.save("order-4", reservation.getId(), AMOUNT);

        // when
        paymentCompleteService.requireCheck(requiresCheckPaymentResult("payment-key-4", "order-4", AMOUNT));

        // then
        entityManager.flush();
        entityManager.clear();

        Reservation updatedReservation = reservationRepository.findById(reservation.getId()).get();
        assertThat(updatedReservation.getReservationStatus()).isEqualTo(REQUIRES_CHECK);

        assertThat(jpaTossPaymentRepository.findAll())
                .singleElement()
                .satisfies(payment -> {
                    assertThat(payment.getReservationId()).isEqualTo(reservation.getId());
                    assertThat(payment.getPaymentKey()).isEqualTo("payment-key-4");
                    assertThat(payment.getOrderId()).isEqualTo("order-4");
                    assertThat(payment.getAmount()).isEqualTo(AMOUNT);
                    assertThat(payment.getPaymentStatus()).isEqualTo(roomescape.payment.domain.PaymentStatus.REQUIRES_CHECK);
                    assertThat(payment.getRequestedAt()).isEqualTo(REQUESTED_AT);
                    assertThat(payment.getApprovedAt()).isNull();
                });

        assertThat(paymentCheckUseCase.findAllByGuestName("브라운"))
                .singleElement()
                .satisfies(result -> {
                    assertThat(result).extracting(
                            PaymentCheckResult::reservationId,
                            PaymentCheckResult::guestName,
                            PaymentCheckResult::date,
                            PaymentCheckResult::paymentKey,
                            PaymentCheckResult::orderId,
                            PaymentCheckResult::amount
                    ).containsExactly(
                            reservation.getId(),
                            "브라운",
                            LocalDate.of(2025, 5, 2),
                            "payment-key-4",
                            "order-4",
                            AMOUNT
                    );
                });
    }

    @Test
    @DisplayName("결제 확인 필요 상태의 예약을 다시 승인하면 기존 결제 정보를 갱신하고 예약을 확정한다.")
    void complete_success_afterRequiresCheck() {
        // given
        Reservation reservation = insertReservation(PENDING);
        paymentSessionRepository.save("order-5", reservation.getId(), AMOUNT);
        paymentCompleteService.requireCheck(requiresCheckPaymentResult("payment-key-5", "order-5", AMOUNT));

        // when
        paymentCompleteService.complete(paymentResult("payment-key-5", "order-5", AMOUNT));

        // then
        entityManager.flush();
        entityManager.clear();

        Reservation updatedReservation = reservationRepository.findById(reservation.getId()).get();
        assertThat(updatedReservation.getReservationStatus()).isEqualTo(CONFIRMED);

        assertThat(jpaTossPaymentRepository.findAll())
                .singleElement()
                .satisfies(payment -> {
                    assertThat(payment.getPaymentKey()).isEqualTo("payment-key-5");
                    assertThat(payment.getOrderId()).isEqualTo("order-5");
                    assertThat(payment.getPaymentStatus()).isEqualTo(DONE);
                    assertThat(payment.getApprovedAt()).isEqualTo(APPROVED_AT);
                });
    }

    @Test
    @DisplayName("결제 만료 시각이 지난 예약은 결제 승인 전에 예약을 취소하고 만료된 결제 예외가 발생한다.")
    void validateCompletable_fail_expiredPendingReservation() {
        // given
        LocalDateTime now = LocalDateTime.of(2025, 5, 1, 12, 0);
        clock.setFixed(now);

        Reservation reservation = insertReservation(PENDING, now.minusMinutes(11), now.minusMinutes(1));

        // when, then
        assertThatThrownBy(() -> paymentCompleteService.validateCompletable(reservation.getId()))
                .isInstanceOf(DomainException.class)
                .hasMessage(PAYMENT_EXPIRED.message());

        entityManager.flush();
        entityManager.clear();

        assertThat(reservationRepository.findById(reservation.getId()).get().getReservationStatus()).isEqualTo(CANCELED);
    }

    private Reservation insertReservation(ReservationStatus reservationStatus) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime paymentExpiresAt = null;
        if (PENDING.equals(reservationStatus)) {
            paymentExpiresAt = now.plusMinutes(10);
        }
        return insertReservation(reservationStatus, now, paymentExpiresAt);
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

    private PaymentResult paymentResult(String paymentKey, String orderId, Long amount) {
        return new PaymentResult(paymentKey, orderId, DONE, amount, APPROVED_AT, REQUESTED_AT);
    }

    private PaymentResult requiresCheckPaymentResult(String paymentKey, String orderId, Long amount) {
        return PaymentResult.requiresCheck(paymentKey, orderId, amount, REQUESTED_AT);
    }
}

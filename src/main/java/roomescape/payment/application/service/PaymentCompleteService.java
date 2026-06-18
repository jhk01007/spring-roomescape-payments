package roomescape.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.DomainException;
import roomescape.payment.application.port.out.PaymentRepository;
import roomescape.payment.application.port.out.PaymentSessionInfo;
import roomescape.payment.application.port.out.PaymentSessionRepository;
import roomescape.payment.domain.Payment;
import roomescape.payment.domain.PaymentResult;
import roomescape.payment.domain.PaymentStatus;
import roomescape.payment.domain.exception.PaymentAmountMismatchException;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;

import java.time.Clock;
import java.time.LocalDateTime;

import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_CONFIRMATION_NOT_DONE;
import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_EXPIRED;
import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_RESERVATION_NOT_PENDING;
import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_SESSION_NOT_FOUND;
import static roomescape.reservation.exception.ReservationErrorCode.RESERVATION_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PaymentCompleteService {

    private final PaymentRepository paymentRepository;
    private final PaymentSessionRepository paymentSessionRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    @Transactional
    public void validateCompletable(Long reservationId) {
        Reservation reservation = getReservation(reservationId);
        validateNotExpired(reservation);
    }

    @Transactional
    public void complete(PaymentResult paymentResult) {
        validateApproved(paymentResult);

        PaymentSessionInfo paymentSession = paymentSessionRepository.findById(paymentResult.orderId())
                .orElseThrow(() -> new DomainException(PAYMENT_SESSION_NOT_FOUND));
        if (!paymentSession.isSameAmount(paymentResult.approvedAmount())) {
            throw new PaymentAmountMismatchException(paymentSession.amount(), paymentResult.approvedAmount());
        }

        Reservation reservation = getReservation(paymentSession.reservationId());

        validatePendingStatus(reservation);

        Payment payment = Payment.approve(
                reservation.getId(),
                paymentResult.paymentKey(),
                paymentResult.orderId(),
                paymentResult.approvedAmount(),
                paymentResult.requestedAt(),
                paymentResult.approvedAt()
        );

        paymentRepository.save(payment);
        reservation.confirm();
    }

    private Reservation getReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new DomainException(RESERVATION_NOT_FOUND));
    }

    private void validateNotExpired(Reservation reservation) {
        if (reservation.isCanceled()) {
            throw new DomainException(PAYMENT_EXPIRED);
        }
        if (reservation.isPaymentExpired(LocalDateTime.now(clock))) {
            reservation.cancel();
            throw new DomainException(PAYMENT_EXPIRED);
        }
    }

    private void validatePendingStatus(Reservation reservation) {
        if (reservation.isCanceled()) {
            throw new DomainException(PAYMENT_EXPIRED);
        }
        if (!reservation.isPending()) {
            throw new DomainException(PAYMENT_RESERVATION_NOT_PENDING);
        }
    }

    private void validateApproved(PaymentResult paymentResult) {
        if (!PaymentStatus.DONE.equals(paymentResult.status())) {
            throw new DomainException(PAYMENT_CONFIRMATION_NOT_DONE);
        }
    }
}

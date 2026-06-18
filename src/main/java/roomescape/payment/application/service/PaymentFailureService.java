package roomescape.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.DomainException;
import roomescape.payment.application.port.in.PaymentFailureUseCase;
import roomescape.payment.application.port.in.dto.PaymentFailureResult;
import roomescape.payment.application.port.out.PaymentSessionInfo;
import roomescape.payment.application.port.out.PaymentSessionRepository;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;

import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_FAILURE_NOT_ALLOWED;
import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_SESSION_NOT_FOUND;
import static roomescape.reservation.exception.ReservationErrorCode.RESERVATION_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PaymentFailureService implements PaymentFailureUseCase {

    private final PaymentSessionRepository paymentSessionRepository;
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional
    public PaymentFailureResult fail(String orderId) {
        PaymentSessionInfo paymentSession = paymentSessionRepository.findById(orderId)
                .orElseThrow(() -> new DomainException(PAYMENT_SESSION_NOT_FOUND));
        Reservation reservation = reservationRepository.findById(paymentSession.reservationId())
                .orElseThrow(() -> new DomainException(RESERVATION_NOT_FOUND));

        if (reservation.isPending()) {
            reservation.cancel();
        }
        if (!reservation.isCanceled()) {
            throw new DomainException(PAYMENT_FAILURE_NOT_ALLOWED);
        }

        return new PaymentFailureResult(orderId, reservation.getId(), reservation.getStatus());
    }
}

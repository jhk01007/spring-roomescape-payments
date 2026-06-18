package roomescape.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.DomainException;
import roomescape.payment.application.port.in.dto.PaymentPrepareCommand;
import roomescape.payment.application.port.in.dto.PaymentPrepareResult;
import roomescape.payment.application.port.in.PaymentPrepareUseCase;
import roomescape.payment.application.port.out.PaymentSessionRepository;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;
import roomescape.theme.domain.Theme;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_EXPIRED;
import static roomescape.payment.domain.exception.PaymentErrorCode.PAYMENT_RESERVATION_NOT_PENDING;
import static roomescape.reservation.exception.ReservationErrorCode.RESERVATION_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PaymentPrepareService implements PaymentPrepareUseCase {

    private final ReservationRepository reservationRepository;
    private final PaymentSessionRepository paymentSessionRepository;
    private final Clock clock;

    @Override
    @Transactional
    public PaymentPrepareResult prepare(PaymentPrepareCommand command) {
        Reservation reservation = getReservation(command.reservationId());
        validatePending(reservation);

        Theme theme = reservation.getTheme();
        Long amount = theme.getPrice();
        String orderName = theme.getName();

        String orderId = createOrderId();
        paymentSessionRepository.save(orderId, reservation.getId(), amount);

        return new PaymentPrepareResult(orderId, reservation.getId(), amount, orderName);
    }

    private Reservation getReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new DomainException(RESERVATION_NOT_FOUND));
    }

    private void validatePending(Reservation reservation) {
        if (reservation.isCanceled()) {
            throw new DomainException(PAYMENT_EXPIRED);
        }
        if (reservation.isPaymentExpired(LocalDateTime.now(clock))) {
            reservation.cancel();
            throw new DomainException(PAYMENT_EXPIRED);
        }
        if (!reservation.isPending()) {
            throw new DomainException(PAYMENT_RESERVATION_NOT_PENDING);
        }
    }

    private String createOrderId() {
        return "order-" + UUID.randomUUID().toString().replace("-", "");
    }
}

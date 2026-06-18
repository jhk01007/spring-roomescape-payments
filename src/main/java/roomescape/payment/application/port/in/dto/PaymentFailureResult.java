package roomescape.payment.application.port.in.dto;

import roomescape.reservation.domain.ReservationStatus;

public record PaymentFailureResult(
        String orderId,
        Long reservationId,
        ReservationStatus reservationStatus
) {
}

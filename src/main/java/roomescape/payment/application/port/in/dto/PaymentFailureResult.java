package roomescape.payment.application.port.in.dto;

import roomescape.reservation.domain.Status;

public record PaymentFailureResult(
        String orderId,
        Long reservationId,
        Status reservationStatus
) {
}

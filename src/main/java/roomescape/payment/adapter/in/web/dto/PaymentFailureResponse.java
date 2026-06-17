package roomescape.payment.adapter.in.web.dto;

import roomescape.payment.application.port.in.dto.PaymentFailureResult;
import roomescape.reservation.domain.Status;

public record PaymentFailureResponse(
        String orderId,
        Long reservationId,
        Status reservationStatus
) {

    public static PaymentFailureResponse from(PaymentFailureResult result) {
        return new PaymentFailureResponse(
                result.orderId(),
                result.reservationId(),
                result.reservationStatus()
        );
    }
}

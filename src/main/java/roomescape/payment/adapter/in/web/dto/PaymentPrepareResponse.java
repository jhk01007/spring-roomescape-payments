package roomescape.payment.adapter.in.web.dto;

import roomescape.payment.application.port.in.dto.PaymentPrepareResult;

public record PaymentPrepareResponse(
        String orderId,
        Long reservationId,
        Long amount,
        String orderName
) {

    public static PaymentPrepareResponse from(PaymentPrepareResult result) {
        return new PaymentPrepareResponse(
                result.orderId(),
                result.reservationId(),
                result.amount(),
                result.orderName()
        );
    }
}

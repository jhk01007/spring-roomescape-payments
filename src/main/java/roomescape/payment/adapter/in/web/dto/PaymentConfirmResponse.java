package roomescape.payment.adapter.in.web.dto;

import roomescape.payment.domain.PaymentResult;
import roomescape.payment.domain.PaymentStatus;

public record PaymentConfirmResponse(
        String paymentKey,
        String orderId,
        PaymentStatus status,
        Long amount
) {

    public static PaymentConfirmResponse from(PaymentResult result) {
        return new PaymentConfirmResponse(
                result.paymentKey(),
                result.orderId(),
                result.status(),
                result.approvedAmount()
        );
    }
}

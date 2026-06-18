package roomescape.payment.adapter.in.web.dto;

import roomescape.payment.application.port.in.dto.PaymentCheckResult;

import java.util.List;

public record PaymentCheckListResponse(
        List<PaymentCheckResponse> payments
) {

    public static PaymentCheckListResponse from(List<PaymentCheckResult> results) {
        return new PaymentCheckListResponse(
                results.stream()
                        .map(PaymentCheckResponse::from)
                        .toList()
        );
    }
}

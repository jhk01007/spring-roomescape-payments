package roomescape.payment.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentFailureRequest(
        @NotBlank(message = "주문 id는 비어 있을 수 없습니다.")
        String orderId
) {
}

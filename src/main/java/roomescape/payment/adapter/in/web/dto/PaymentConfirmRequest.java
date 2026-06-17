package roomescape.payment.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentConfirmRequest(
        @NotBlank(message = "결제 키는 비어 있을 수 없습니다.")
        String paymentKey,

        @NotBlank(message = "주문 id는 비어 있을 수 없습니다.")
        String orderId,

        @NotNull(message = "결제 금액은 비어 있을 수 없습니다.")
        @Positive(message = "결제 금액은 0보다 커야 합니다.")
        Long amount
) {
}

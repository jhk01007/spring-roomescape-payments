package roomescape.payment.adapter.out.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

/**
 * Toss 결제 승인 성공 응답. 실제 응답의 모르는 필드는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentResponse(
    String paymentKey,
    String orderId,
    String orderName,
    String status,
    Long totalAmount,
    Long balanceAmount,
    String method,
    OffsetDateTime approvedAt,
    OffsetDateTime requestedAt
) {

}

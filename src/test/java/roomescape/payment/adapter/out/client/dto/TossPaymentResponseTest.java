package roomescape.payment.adapter.out.client.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TossPaymentResponseTest {

    @Test
    @DisplayName("토스 승인 응답의 오프셋 시간이 포함된 시각 문자열을 읽는다.")
    void parseOffsetDateTimeText() throws Exception {
        // given
        ObjectMapper objectMapper = new ObjectMapper();
        String response = """
                {
                  "paymentKey": "payment-key",
                  "orderId": "order-id",
                  "orderName": "레벨2 탈출",
                  "status": "DONE",
                  "totalAmount": 43000,
                  "balanceAmount": 43000,
                  "method": "카드",
                  "requestedAt": "2026-06-17T23:25:10+09:00",
                  "approvedAt": "2026-06-17T23:26:10+09:00"
                }
                """;

        // when
        TossPaymentResponse tossPaymentResponse = objectMapper.readValue(response, TossPaymentResponse.class);

        // then
        assertThat(tossPaymentResponse.requestedAt()).isEqualTo("2026-06-17T23:25:10+09:00");
        assertThat(tossPaymentResponse.approvedAt()).isEqualTo("2026-06-17T23:26:10+09:00");
    }
}

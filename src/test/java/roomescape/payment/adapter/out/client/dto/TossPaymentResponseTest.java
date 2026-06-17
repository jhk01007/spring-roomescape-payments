package roomescape.payment.adapter.out.client.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TossPaymentResponseTest {

    @Test
    @DisplayName("토스 승인 응답의 오프셋 시간이 포함된 시각을 파싱한다.")
    void parseOffsetDateTime() throws Exception {
        // given
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
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
        assertThat(tossPaymentResponse.requestedAt())
                .isEqualTo(OffsetDateTime.parse("2026-06-17T23:25:10+09:00"));
        assertThat(tossPaymentResponse.approvedAt())
                .isEqualTo(OffsetDateTime.parse("2026-06-17T23:26:10+09:00"));
    }
}

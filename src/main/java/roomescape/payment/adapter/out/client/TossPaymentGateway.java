package roomescape.payment.adapter.out.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import roomescape.payment.adapter.out.client.dto.ConfirmRequest;
import roomescape.payment.adapter.out.client.dto.TossErrorResponse;
import roomescape.payment.adapter.out.client.dto.TossPaymentResponse;
import roomescape.payment.application.port.out.PaymentGateway;
import roomescape.payment.domain.PaymentConfirmation;
import roomescape.payment.domain.PaymentResult;
import roomescape.payment.domain.PaymentStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * PaymentGateway 포트의 Toss 구현(어댑터). Toss 의 요청·응답·에러 포맷은 이 클래스 밖으로 새어 나가지 않는다(부패 방지 계층).
 */
@Component
public class TossPaymentGateway implements PaymentGateway {

    private final RestClient tossRestClient;
    private final ObjectMapper objectMapper;

    public TossPaymentGateway(RestClient tossRestClient, ObjectMapper objectMapper) {
        this.tossRestClient = tossRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentResult confirm(PaymentConfirmation confirmation) {

        ConfirmRequest confirmRequest = new ConfirmRequest(confirmation.paymentKey(), confirmation.orderId(), confirmation.amount());
        TossPaymentResponse tossPaymentResponse = tossRestClient.post()
                .uri("/v1/payments/confirm")
                .body(confirmRequest)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            TossErrorResponse error = objectMapper.readValue(response.getBody(), TossErrorResponse.class);
                            throw TossPaymentException.of(response.getStatusCode(), error);
                        }
                )
                .body(TossPaymentResponse.class);
        return toResult(tossPaymentResponse);
    }

    private static PaymentResult toResult(TossPaymentResponse tossPaymentResponse) {
        return new PaymentResult(
                tossPaymentResponse.paymentKey(),
                tossPaymentResponse.orderId(),
                PaymentStatus.from(tossPaymentResponse.status()),
                tossPaymentResponse.totalAmount(),
                toLocalDateTime(tossPaymentResponse.approvedAt()),
                toLocalDateTime(tossPaymentResponse.requestedAt())
        );
    }

    private static LocalDateTime toLocalDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDateTime();
    }

}

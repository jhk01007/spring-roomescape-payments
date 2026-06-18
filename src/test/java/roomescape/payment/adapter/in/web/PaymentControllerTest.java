package roomescape.payment.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import roomescape.payment.adapter.out.client.TossPaymentException;
import roomescape.payment.adapter.in.web.dto.PaymentConfirmRequest;
import roomescape.payment.adapter.in.web.dto.PaymentFailureRequest;
import roomescape.payment.application.port.in.PaymentConfirmUseCase;
import roomescape.payment.application.port.in.PaymentFailureUseCase;
import roomescape.payment.application.port.in.dto.PaymentFailureResult;
import roomescape.payment.domain.PaymentResult;
import roomescape.test_config.integration.controller.ControllerTest;
import roomescape.test_config.integration.controller.MockedBean;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static roomescape.payment.domain.PaymentStatus.DONE;
import static roomescape.reservation.domain.Status.CANCELED;

@ControllerTest
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockedBean
    private PaymentConfirmUseCase paymentConfirmUseCase;

    @MockedBean
    private PaymentFailureUseCase paymentFailureUseCase;

    @Test
    @DisplayName("결제 승인 요청을 처리한다.")
    void confirm_success() throws Exception {
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest("payment-key", "order-id", 40_000L);
        given(paymentConfirmUseCase.confirm("payment-key", "order-id", 40_000L))
                .willReturn(new PaymentResult(
                        "payment-key",
                        "order-id",
                        DONE,
                        40_000L,
                        LocalDateTime.of(2026, 6, 17, 23, 40),
                        LocalDateTime.of(2026, 6, 17, 23, 39)
                ));

        // when, then
        mockMvc.perform(post("/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentKey").value("payment-key"))
                .andExpect(jsonPath("$.orderId").value("order-id"))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.amount").value(40_000));

        then(paymentConfirmUseCase)
                .should()
                .confirm("payment-key", "order-id", 40_000L);
    }

    @Test
    @DisplayName("Toss 결제 예외가 발생하면 Toss 예외 응답 형식으로 반환한다.")
    void confirm_fail_tossPaymentException() throws Exception {
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest("payment-key", "order-id", 40_000L);
        given(paymentConfirmUseCase.confirm("payment-key", "order-id", 40_000L))
                .willThrow(new TossPaymentException.AlreadyProcessed("이미 처리된 결제 입니다."));

        // when, then
        mockMvc.perform(post("/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/payments/confirm"))
                .andExpect(jsonPath("$.code").value("ALREADY_PROCESSED_PAYMENT"))
                .andExpect(jsonPath("$.message").value("이미 처리된 결제 입니다."));
    }

    @Test
    @DisplayName("결제 실패 요청을 처리한다.")
    void fail_success() throws Exception {
        // given
        PaymentFailureRequest request = new PaymentFailureRequest("order-id");
        given(paymentFailureUseCase.fail("order-id"))
                .willReturn(new PaymentFailureResult("order-id", 1L, CANCELED));

        // when, then
        mockMvc.perform(post("/payments/failures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-id"))
                .andExpect(jsonPath("$.reservationId").value(1L))
                .andExpect(jsonPath("$.reservationStatus").value("CANCELED"));

        then(paymentFailureUseCase)
                .should()
                .fail("order-id");
    }
}

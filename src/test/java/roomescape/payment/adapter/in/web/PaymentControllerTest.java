package roomescape.payment.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import roomescape.payment.application.port.in.PaymentConfirmUseCase;
import roomescape.payment.domain.PaymentResult;
import roomescape.test_config.integration.controller.ControllerTest;
import roomescape.test_config.integration.controller.MockedBean;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static roomescape.payment.domain.PaymentStatus.DONE;

@ControllerTest
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockedBean
    private PaymentConfirmUseCase paymentConfirmUseCase;

    @Test
    @DisplayName("결제 성공 콜백을 처리하면 예약 화면으로 리다이렉트한다.")
    void success_redirectToReservationPage() throws Exception {
        // given
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
        mockMvc.perform(get("/payments/success")
                        .param("paymentKey", "payment-key")
                        .param("orderId", "order-id")
                        .param("amount", "40000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?payment=success"));

        then(paymentConfirmUseCase)
                .should()
                .confirm("payment-key", "order-id", 40_000L);
    }
}

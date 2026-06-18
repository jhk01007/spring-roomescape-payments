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
import roomescape.payment.application.port.in.PaymentCheckUseCase;
import roomescape.payment.application.port.in.PaymentConfirmUseCase;
import roomescape.payment.application.port.in.PaymentFailureUseCase;
import roomescape.payment.application.port.in.dto.PaymentCheckResult;
import roomescape.payment.application.port.in.dto.PaymentFailureResult;
import roomescape.payment.domain.PaymentResult;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.test_config.integration.controller.ControllerTest;
import roomescape.test_config.integration.controller.MockedBean;
import roomescape.theme.domain.Theme;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static roomescape.common.auth.UserArgumentResolver.GUEST_NAME_HEADER;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static roomescape.payment.domain.PaymentStatus.DONE;
import static roomescape.payment.domain.PaymentStatus.REQUIRES_CHECK;
import static roomescape.reservation.domain.ReservationStatus.CANCELED;

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

    @MockedBean
    private PaymentCheckUseCase paymentCheckUseCase;

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
    @DisplayName("사용자별 결제 정보 전체 목록을 조회한다.")
    void getMyPayments_success() throws Exception {
        // given
        ReservationTime time = ReservationTime.of(1L, LocalTime.of(10, 0));
        Theme theme = Theme.of(1L, "레벨2 탈출", "우테코 레벨2를 탈출하는 내용입니다.", "https://example.com/theme.png");
        PaymentCheckResult result = new PaymentCheckResult(
                1L,
                "브라운",
                LocalDate.of(2026, 6, 20),
                time,
                theme,
                REQUIRES_CHECK,
                "payment-key",
                "order-id",
                40_000L
        );
        PaymentCheckResult completedResult = new PaymentCheckResult(
                2L,
                "브라운",
                LocalDate.of(2026, 6, 19),
                time,
                theme,
                DONE,
                "done-payment-key",
                "done-order-id",
                40_000L
        );
        given(paymentCheckUseCase.findAllByGuestName("브라운"))
                .willReturn(List.of(result, completedResult));

        // when, then
        mockMvc.perform(get("/payments/me")
                        .header(GUEST_NAME_HEADER, "브라운"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments[0].reservationId").value(1L))
                .andExpect(jsonPath("$.payments[0].guestName").value("브라운"))
                .andExpect(jsonPath("$.payments[0].status").value("REQUIRES_CHECK"))
                .andExpect(jsonPath("$.payments[0].paymentKey").value("payment-key"))
                .andExpect(jsonPath("$.payments[0].orderId").value("order-id"))
                .andExpect(jsonPath("$.payments[0].amount").value(40_000L))
                .andExpect(jsonPath("$.payments[1].status").value("DONE"))
                .andExpect(jsonPath("$.payments[1].paymentKey").value("done-payment-key"))
                .andExpect(jsonPath("$.payments[1].orderId").value("done-order-id"));

        then(paymentCheckUseCase)
                .should()
                .findAllByGuestName("브라운");
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

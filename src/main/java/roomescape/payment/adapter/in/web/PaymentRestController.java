package roomescape.payment.adapter.in.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.payment.adapter.in.web.dto.PaymentPrepareRequest;
import roomescape.payment.adapter.in.web.dto.PaymentPrepareResponse;
import roomescape.payment.application.port.in.PaymentPrepareResult;
import roomescape.payment.application.port.in.PaymentPrepareUseCase;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentRestController {
    private final PaymentPrepareUseCase paymentPrepareUseCase;

    @PostMapping("/prepare")
    public ResponseEntity<PaymentPrepareResponse> prepare(
            HttpSession session,
            @RequestBody @Valid PaymentPrepareRequest request
    ) {
        PaymentPrepareResult result = paymentPrepareUseCase.prepare(request.toCommand());
        savePaymentAmountInSession(session, result.orderId(), result.amount());
        return ResponseEntity.ok(PaymentPrepareResponse.from(result));
    }

    private static void savePaymentAmountInSession(HttpSession session, String orderId, Long amount) {
        session.setAttribute(orderId, amount);
    }
}

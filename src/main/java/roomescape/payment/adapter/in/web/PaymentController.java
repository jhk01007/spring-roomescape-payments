package roomescape.payment.adapter.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import roomescape.common.exception.DomainException;
import roomescape.payment.adapter.out.client.TossPaymentException;
import roomescape.payment.application.port.in.PaymentConfirmUseCase;
import roomescape.payment.domain.exception.PaymentAmountMismatchException;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentConfirmUseCase paymentConfirmUseCase;

    @GetMapping("/success")
    public String success(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            Model model
    ) {
        try {
            paymentConfirmUseCase.confirm(paymentKey, orderId, amount);
            return "redirect:/?payment=success";
        } catch (PaymentAmountMismatchException e) {
            return failView(model, "AMOUNT_MISMATCH", e.getMessage(), orderId);
        } catch (TossPaymentException e) {
            return failView(model, e.getCode(), e.getMessage(), orderId);
        } catch (DomainException e) {
            return failView(model, e.getErrorPolicy().code(), e.getMessage(), orderId);
        }
    }

    @GetMapping("/fail")
    public String fail(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String orderId,
            Model model
    ) {
        return failView(model, code, message, orderId);
    }

    private String failView(Model model, String code, String message, String orderId) {
        model.addAttribute("code", code);
        model.addAttribute("message", message);
        model.addAttribute("orderId", orderId);
        return "payments/fail";
    }
}

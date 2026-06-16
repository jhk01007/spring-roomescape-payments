package roomescape.page;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequestMapping("/payments")
public class PaymentPageController {

    private static final String DEFAULT_ORDER_NAME = "방탈출 예약";
    private static final long DEFAULT_AMOUNT = 50_000L;

    private final String clientKey;

    public PaymentPageController(@Value("${toss.client-key:}") String clientKey) {
        this.clientKey = clientKey;
    }

    @GetMapping({"", "/"})
    public String checkout(Model model) {
        model.addAttribute("clientKey", clientKey);
        model.addAttribute("amount", DEFAULT_AMOUNT);
        return "payments/checkout";
    }

    @GetMapping("/success")
    public String success(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            Model model
    ) {
        model.addAttribute("paymentKey", paymentKey);
        model.addAttribute("orderId", orderId);
        model.addAttribute("amount", amount);
        return "payments/success";
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

package roomescape.page;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private static final String ASSET_VERSION = "checkout-payment-1";

    private final String tossClientKey;

    public PageController(@Value("${toss.client-key:}") String tossClientKey) {
        this.tossClientKey = tossClientKey;
    }

    @GetMapping("/")
    public String index(Model model) {
        addLayoutAttributes(model, "user", "방탈출 예약", "날짜와 테마를 고르고 가능한 시간만 예약");
        return "index";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        addLayoutAttributes(model, "admin", "방탈출 예약 관리자", "테마, 시간, 예약을 운영 관리");
        return "admin";
    }

    @GetMapping("/payments")
    public String payment(Model model) {
        addLayoutAttributes(model, "payment", "방탈출 예약 결제", "결제수단을 선택하고 예약을 확정");
        return "payment";
    }

    @GetMapping("/index.html")
    public String redirectIndex() {
        return "redirect:/";
    }

    @GetMapping("/admin.html")
    public String redirectAdmin() {
        return "redirect:/admin";
    }

    private void addLayoutAttributes(Model model, String activePage, String pageTitle, String brandSubtitle) {
        model.addAttribute("activePage", activePage);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("brandSubtitle", brandSubtitle);
        model.addAttribute("assetVersion", ASSET_VERSION);
        model.addAttribute("tossClientKey", tossClientKey);
    }
}

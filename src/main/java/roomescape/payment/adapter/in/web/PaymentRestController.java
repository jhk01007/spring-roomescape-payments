package roomescape.payment.adapter.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.payment.adapter.in.web.dto.PaymentPrepareRequest;
import roomescape.payment.adapter.in.web.dto.PaymentPrepareResponse;
import roomescape.payment.application.port.in.PaymentPrepareUseCase;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentRestController {
    private final PaymentPrepareUseCase paymentPrepareUseCase;

    @PostMapping("/prepare")
    public ResponseEntity<PaymentPrepareResponse> prepare(
            @RequestBody @Valid PaymentPrepareRequest request
    ) {
        return ResponseEntity.ok(PaymentPrepareResponse.from(paymentPrepareUseCase.prepare(request.toCommand())));
    }
}

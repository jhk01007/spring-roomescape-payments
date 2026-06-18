package roomescape.payment.adapter.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.common.auth.CurrentUser;
import roomescape.payment.adapter.in.web.dto.PaymentCheckListResponse;
import roomescape.payment.adapter.in.web.dto.PaymentConfirmRequest;
import roomescape.payment.adapter.in.web.dto.PaymentConfirmResponse;
import roomescape.payment.adapter.in.web.dto.PaymentFailureRequest;
import roomescape.payment.adapter.in.web.dto.PaymentFailureResponse;
import roomescape.payment.adapter.in.web.dto.PaymentPrepareRequest;
import roomescape.payment.adapter.in.web.dto.PaymentPrepareResponse;
import roomescape.payment.adapter.in.web.dto.PaymentReservationRequest;
import roomescape.payment.adapter.in.web.dto.PaymentReservationResponse;
import roomescape.payment.application.port.in.PaymentCheckUseCase;
import roomescape.payment.application.port.in.PaymentConfirmUseCase;
import roomescape.payment.application.port.in.PaymentFailureUseCase;
import roomescape.payment.application.port.in.PaymentPrepareUseCase;
import roomescape.payment.application.port.in.PaymentReservationUseCase;
import roomescape.payment.application.port.in.dto.PaymentFailureResult;
import roomescape.payment.domain.PaymentResult;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentPrepareUseCase paymentPrepareUseCase;
    private final PaymentReservationUseCase paymentReservationUseCase;
    private final PaymentConfirmUseCase paymentConfirmUseCase;
    private final PaymentFailureUseCase paymentFailureUseCase;
    private final PaymentCheckUseCase paymentCheckUseCase;

    @PostMapping("/reservations")
    public ResponseEntity<PaymentReservationResponse> createReservation(
            @RequestBody @Valid PaymentReservationRequest request
    ) {
        return ResponseEntity.status(CREATED)
                .body(PaymentReservationResponse.from(paymentReservationUseCase.create(request.toCommand())));
    }

    @PostMapping("/prepare")
    public ResponseEntity<PaymentPrepareResponse> prepare(
            @RequestBody @Valid PaymentPrepareRequest request
    ) {
        return ResponseEntity.ok(PaymentPrepareResponse.from(paymentPrepareUseCase.prepare(request.toCommand())));
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirm(
            @RequestBody @Valid PaymentConfirmRequest request
    ) {
        PaymentResult paymentResult = paymentConfirmUseCase.confirm(
                request.paymentKey(),
                request.orderId(),
                request.amount()
        );
        return ResponseEntity.ok(PaymentConfirmResponse.from(paymentResult));
    }

    @GetMapping("/me")
    public ResponseEntity<PaymentCheckListResponse> getMyPayments(@CurrentUser String guestName) {
        return ResponseEntity.ok(PaymentCheckListResponse.from(
                paymentCheckUseCase.findAllByGuestName(guestName)
        ));
    }

    @PostMapping("/failures")
    public ResponseEntity<PaymentFailureResponse> fail(
            @RequestBody @Valid PaymentFailureRequest request
    ) {
        PaymentFailureResult result = paymentFailureUseCase.fail(request.orderId());
        return ResponseEntity.ok(PaymentFailureResponse.from(result));
    }
}

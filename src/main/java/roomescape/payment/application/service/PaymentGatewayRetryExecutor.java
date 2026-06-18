package roomescape.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import roomescape.payment.application.port.out.PaymentGateway;
import roomescape.payment.application.retry.RetryableTossPayment;
import roomescape.payment.domain.PaymentConfirmation;
import roomescape.payment.domain.PaymentResult;

@Service
@RequiredArgsConstructor
public class PaymentGatewayRetryExecutor {

    private final PaymentGateway paymentGateway;

    @RetryableTossPayment
    public PaymentResult confirm(PaymentConfirmation confirmation) {
        return paymentGateway.confirm(confirmation);
    }
}

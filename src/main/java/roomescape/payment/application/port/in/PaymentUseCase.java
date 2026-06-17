package roomescape.payment.application.port.in;

import roomescape.payment.domain.PaymentResult;

public interface PaymentUseCase {
    PaymentResult confirm(String paymentKey, String orderId, Long amount);
}

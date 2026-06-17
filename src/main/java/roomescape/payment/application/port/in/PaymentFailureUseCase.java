package roomescape.payment.application.port.in;

import roomescape.payment.application.port.in.dto.PaymentFailureResult;

public interface PaymentFailureUseCase {

    PaymentFailureResult fail(String orderId);
}

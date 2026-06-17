package roomescape.payment.application.port.in;

import roomescape.payment.application.port.in.dto.PaymentPrepareCommand;
import roomescape.payment.application.port.in.dto.PaymentPrepareResult;

public interface PaymentPrepareUseCase {
    PaymentPrepareResult prepare(PaymentPrepareCommand command);
}

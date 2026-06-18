package roomescape.payment.application.port.in;

import roomescape.payment.application.port.in.dto.PaymentCheckResult;

import java.util.List;

public interface PaymentCheckUseCase {

    List<PaymentCheckResult> findRequiresCheckByGuestName(String guestName);
}

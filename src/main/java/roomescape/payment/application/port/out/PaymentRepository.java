package roomescape.payment.application.port.out;

import roomescape.payment.domain.Payment;
import roomescape.payment.domain.PaymentStatus;

import java.util.List;

public interface PaymentRepository {

    void save(Payment payment);

    boolean existsByPaymentKey(String paymentKey);

    boolean existsByPaymentKeyAndStatus(String paymentKey, PaymentStatus status);

    List<PaymentCheckInfo> findAllByGuestName(String guestName);
}

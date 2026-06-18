package roomescape.payment.application.port.out;

import roomescape.payment.domain.Payment;

public interface PaymentRepository {

    void save(Payment payment);

    boolean existsByPaymentKey(String paymentKey);
}

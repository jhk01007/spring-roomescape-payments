package roomescape.payment.application.port.out;

import java.util.Optional;

public interface PaymentSessionRepository {
    Optional<PaymentSessionInfo> findById(String orderId);
    void save(String orderId, Long reservationId, Long amount);
}

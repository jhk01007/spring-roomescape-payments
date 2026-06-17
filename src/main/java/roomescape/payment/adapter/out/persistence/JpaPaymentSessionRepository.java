package roomescape.payment.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.payment.adapter.out.persistence.entity.PaymentSession;

public interface JpaPaymentSessionRepository extends JpaRepository<PaymentSession, String> {
}

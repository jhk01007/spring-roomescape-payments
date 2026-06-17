package roomescape.payment.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPaymentSessionRepository extends JpaRepository<PaymentSession, String> {
}

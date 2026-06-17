package roomescape.payment.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.payment.adapter.out.persistence.entity.TossPayment;

public interface JpaTossPaymentRepository extends JpaRepository<TossPayment, Long> {
}

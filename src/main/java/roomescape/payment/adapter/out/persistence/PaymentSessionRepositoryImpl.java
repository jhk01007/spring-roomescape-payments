package roomescape.payment.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import roomescape.payment.application.port.out.PaymentSessionRepository;

@Repository
@RequiredArgsConstructor
public class PaymentSessionRepositoryImpl implements PaymentSessionRepository {

    private final JpaPaymentSessionRepository jpaPaymentSessionRepository;

    @Override
    public void save(String orderId, Long reservationId, Long amount) {
        jpaPaymentSessionRepository.save(PaymentSession.create(orderId, reservationId, amount));
    }
}

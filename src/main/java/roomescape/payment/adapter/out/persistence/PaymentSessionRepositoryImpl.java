package roomescape.payment.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import roomescape.payment.adapter.out.persistence.entity.PaymentSession;
import roomescape.payment.application.port.out.PaymentSessionInfo;
import roomescape.payment.application.port.out.PaymentSessionRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentSessionRepositoryImpl implements PaymentSessionRepository {

    private final JpaPaymentSessionRepository jpaPaymentSessionRepository;

    @Override
    public Optional<PaymentSessionInfo> findById(String orderId) {
        return jpaPaymentSessionRepository.findById(orderId)
                .map(paymentSession -> new PaymentSessionInfo(
                        paymentSession.getOrderId(),
                        paymentSession.getReservationId(),
                        paymentSession.getAmount()
                ));
    }

    @Override
    public void save(String orderId, Long reservationId, Long amount) {
        jpaPaymentSessionRepository.save(PaymentSession.create(orderId, reservationId, amount));
    }
}

package roomescape.payment.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import roomescape.payment.adapter.out.persistence.entity.TossPayment;
import roomescape.payment.application.port.out.PaymentRepository;
import roomescape.payment.domain.Payment;
import roomescape.reservation.domain.Reservation;

@Repository
@RequiredArgsConstructor
public class TossPaymentRepository implements PaymentRepository {

    private final JpaTossPaymentRepository jpaTossPaymentRepository;
    private final EntityManager entityManager;

    @Override
    public void save(Payment payment) {
        Reservation reservation = entityManager.getReference(Reservation.class, payment.getReservationId());
        TossPayment tossPayment = TossPayment.create(
                reservation,
                payment.getPaymentKey(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getApprovedAt(),
                payment.getRequestedAt()
        );
        jpaTossPaymentRepository.save(tossPayment);
    }

    @Override
    public boolean existsByPaymentKey(String paymentKey) {
        return jpaTossPaymentRepository.existsByPaymentKey(paymentKey);
    }
}

package roomescape.payment.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import roomescape.payment.adapter.out.persistence.entity.TossPayment;
import roomescape.payment.application.port.out.PaymentRepository;
import roomescape.payment.application.port.out.PaymentCheckInfo;
import roomescape.payment.domain.Payment;
import roomescape.payment.domain.PaymentStatus;
import roomescape.reservation.domain.Reservation;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TossPaymentRepository implements PaymentRepository {

    private final JpaTossPaymentRepository jpaTossPaymentRepository;
    private final EntityManager entityManager;

    @Override
    public void save(Payment payment) {
        jpaTossPaymentRepository.findByPaymentKeyOrOrderId(payment.getPaymentKey(), payment.getOrderId())
                .ifPresentOrElse(
                        tossPayment -> tossPayment.update(
                                payment.getOrderId(),
                                payment.getAmount(),
                                payment.getStatus(),
                                payment.getApprovedAt(),
                                payment.getRequestedAt()
                        ),
                        () -> saveNew(payment)
                );
    }

    @Override
    public boolean existsByPaymentKey(String paymentKey) {
        return jpaTossPaymentRepository.existsByPaymentKey(paymentKey);
    }

    @Override
    public boolean existsByPaymentKeyAndStatus(String paymentKey, PaymentStatus status) {
        return jpaTossPaymentRepository.existsByPaymentKeyAndPaymentStatus(paymentKey, status);
    }

    @Override
    public List<PaymentCheckInfo> findAllRequiresCheckByGuestName(String guestName) {
        return jpaTossPaymentRepository.findAllByGuestNameAndStatus(guestName, PaymentStatus.REQUIRES_CHECK)
                .stream()
                .map(this::toPaymentCheckInfo)
                .toList();
    }

    private void saveNew(Payment payment) {
        Reservation reservation = entityManager.getReference(Reservation.class, payment.getReservationId());
        TossPayment tossPayment = TossPayment.create(
                reservation,
                payment.getPaymentKey(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getApprovedAt(),
                payment.getRequestedAt()
        );
        jpaTossPaymentRepository.save(tossPayment);
    }

    private PaymentCheckInfo toPaymentCheckInfo(TossPayment tossPayment) {
        Reservation reservation = tossPayment.getReservation();
        return new PaymentCheckInfo(
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getDate(),
                reservation.getTime(),
                reservation.getTheme(),
                tossPayment.getPaymentStatus(),
                tossPayment.getPaymentKey(),
                tossPayment.getOrderId(),
                tossPayment.getAmount()
        );
    }
}

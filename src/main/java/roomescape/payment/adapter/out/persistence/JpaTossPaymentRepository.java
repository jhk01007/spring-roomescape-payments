package roomescape.payment.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import roomescape.payment.adapter.out.persistence.entity.TossPayment;
import roomescape.payment.domain.PaymentStatus;

import java.util.List;
import java.util.Optional;

public interface JpaTossPaymentRepository extends JpaRepository<TossPayment, Long> {

    boolean existsByPaymentKey(String paymentKey);

    boolean existsByPaymentKeyAndPaymentStatus(String paymentKey, PaymentStatus reservationStatus);

    Optional<TossPayment> findByPaymentKeyOrOrderId(String paymentKey, String orderId);

    @Query("""
            SELECT payment
            FROM TossPayment payment
            JOIN FETCH payment.reservation reservation
            JOIN FETCH reservation.time
            JOIN FETCH reservation.theme
            WHERE reservation.guestName = :guestName
              AND payment.paymentStatus = :paymentStatus
            ORDER BY reservation.date DESC, payment.id DESC
            """)
    List<TossPayment> findAllByGuestNameAndStatus(
            @Param("guestName") String guestName,
            @Param("paymentStatus") PaymentStatus paymentStatus
    );
}

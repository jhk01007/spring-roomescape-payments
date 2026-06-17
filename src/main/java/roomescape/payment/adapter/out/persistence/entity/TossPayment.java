package roomescape.payment.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import roomescape.payment.domain.PaymentStatus;
import roomescape.reservation.domain.Reservation;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(name = "toss_payment")
public class TossPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "payment_key", nullable = false)
    private String paymentKey;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "approved_at", nullable = false)
    private LocalDateTime approvedAt;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    protected TossPayment() {
    }

    private TossPayment(
            Long id,
            Reservation reservation,
            String paymentKey,
            Long amount,
            PaymentStatus status,
            LocalDateTime approvedAt,
            LocalDateTime requestedAt
    ) {
        validate(reservation, paymentKey, amount, status, approvedAt, requestedAt);
        this.id = id;
        this.reservation = reservation;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.status = status;
        this.approvedAt = approvedAt;
        this.requestedAt = requestedAt;
    }

    public static TossPayment create(
            Reservation reservation,
            String paymentKey,
            Long amount,
            PaymentStatus status,
            LocalDateTime approvedAt,
            LocalDateTime requestedAt
    ) {
        return new TossPayment(null, reservation, paymentKey, amount, status, approvedAt, requestedAt);
    }

    public static TossPayment of(
            Long id,
            Reservation reservation,
            String paymentKey,
            Long amount,
            PaymentStatus status,
            LocalDateTime approvedAt,
            LocalDateTime requestedAt
    ) {
        return new TossPayment(id, reservation, paymentKey, amount, status, approvedAt, requestedAt);
    }

    public Long getReservationId() {
        return reservation.getId();
    }

    private void validate(
            Reservation reservation,
            String paymentKey,
            Long amount,
            PaymentStatus status,
            LocalDateTime approvedAt,
            LocalDateTime requestedAt
    ) {
        if (reservation == null) {
            throw new IllegalArgumentException("예약은 필수입니다.");
        }
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new IllegalArgumentException("결제 키는 필수입니다.");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 양수여야 합니다.");
        }
        if (status == null) {
            throw new IllegalArgumentException("결제 상태는 필수입니다.");
        }
        if (approvedAt == null) {
            throw new IllegalArgumentException("결제 승인 시각은 필수입니다.");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("결제 요청 시각은 필수입니다.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TossPayment that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

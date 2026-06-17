package roomescape.payment.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.Objects;

@Getter
@Entity
@Table(name = "payment_session")
public class PaymentSession {

    @Id
    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(nullable = false)
    private Long amount;

    protected PaymentSession() {
    }

    private PaymentSession(String orderId, Long reservationId, Long amount) {
        validate(orderId, reservationId, amount);
        this.orderId = orderId;
        this.reservationId = reservationId;
        this.amount = amount;
    }

    public static PaymentSession create(String orderId, Long reservationId, Long amount) {
        return new PaymentSession(orderId, reservationId, amount);
    }

    private void validate(String orderId, Long reservationId, Long amount) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("주문 id는 필수입니다.");
        }
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("예약 id는 양수여야 합니다.");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 양수여야 합니다.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentSession that)) {
            return false;
        }
        return orderId != null && Objects.equals(orderId, that.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(orderId);
    }

    public boolean isSameAmount(long amount) {
        return Objects.equals(this.amount, amount);
    }
}

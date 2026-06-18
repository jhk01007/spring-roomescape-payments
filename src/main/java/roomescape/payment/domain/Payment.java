package roomescape.payment.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Payment {
    private final Long id;
    private final Long reservationId;
    private final String paymentKey;
    private final String orderId;
    private final Long amount;
    private PaymentStatus status;
    private final LocalDateTime requestedAt;
    private LocalDateTime approvedAt;

    public static Payment approve(
            Long reservationId,
            String paymentKey,
            String orderId,
            Long amount,
            LocalDateTime requestedAt,
            LocalDateTime approvedAt
    ) {
        return new Payment(
                null,
                reservationId,
                paymentKey,
                orderId,
                amount,
                PaymentStatus.DONE,
                requestedAt,
                approvedAt
        );
    }

    public static Payment requiresCheck(
            Long reservationId,
            String paymentKey,
            String orderId,
            Long amount,
            LocalDateTime requestedAt
    ) {
        return new Payment(
                null,
                reservationId,
                paymentKey,
                orderId,
                amount,
                PaymentStatus.REQUIRES_CHECK,
                requestedAt,
                null
        );
    }
}

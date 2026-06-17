package roomescape.payment.application.port.out;

import java.util.Objects;

public record PaymentSessionInfo(
        String orderId,
        Long reservationId,
        Long amount
) {

    public boolean isSameAmount(Long amount) {
        return Objects.equals(this.amount, amount);
    }
}

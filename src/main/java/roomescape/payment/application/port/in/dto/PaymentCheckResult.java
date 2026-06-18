package roomescape.payment.application.port.in.dto;

import roomescape.payment.application.port.out.PaymentCheckInfo;
import roomescape.payment.domain.PaymentStatus;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.theme.domain.Theme;

import java.time.LocalDate;

public record PaymentCheckResult(
        Long reservationId,
        String guestName,
        LocalDate date,
        ReservationTime time,
        Theme theme,
        PaymentStatus status,
        String paymentKey,
        String orderId,
        Long amount
) {

    public static PaymentCheckResult from(PaymentCheckInfo paymentCheckInfo) {
        return new PaymentCheckResult(
                paymentCheckInfo.reservationId(),
                paymentCheckInfo.guestName(),
                paymentCheckInfo.date(),
                paymentCheckInfo.time(),
                paymentCheckInfo.theme(),
                paymentCheckInfo.status(),
                paymentCheckInfo.paymentKey(),
                paymentCheckInfo.orderId(),
                paymentCheckInfo.amount()
        );
    }
}

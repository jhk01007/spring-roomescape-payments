package roomescape.payment.application.port.out;

import roomescape.payment.domain.PaymentStatus;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.theme.domain.Theme;

import java.time.LocalDate;

public record PaymentCheckInfo(
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
}

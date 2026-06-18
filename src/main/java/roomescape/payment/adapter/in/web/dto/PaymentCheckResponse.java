package roomescape.payment.adapter.in.web.dto;

import roomescape.payment.application.port.in.dto.PaymentCheckResult;
import roomescape.payment.domain.PaymentStatus;
import roomescape.reservationtime.controller.dto.ReservationTimeResponse;
import roomescape.theme.controller.dto.ThemeResponse;

public record PaymentCheckResponse(
        Long reservationId,
        String guestName,
        String date,
        ReservationTimeResponse time,
        ThemeResponse theme,
        PaymentStatus status,
        String paymentKey,
        String orderId,
        Long amount
) {

    public static PaymentCheckResponse from(PaymentCheckResult result) {
        return new PaymentCheckResponse(
                result.reservationId(),
                result.guestName(),
                result.date().toString(),
                ReservationTimeResponse.from(result.time()),
                ThemeResponse.from(result.theme()),
                result.status(),
                result.paymentKey(),
                result.orderId(),
                result.amount()
        );
    }
}

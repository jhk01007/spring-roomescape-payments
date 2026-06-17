package roomescape.payment.adapter.in.web.dto;

import roomescape.payment.application.port.in.dto.PaymentReservationResult;

public record PaymentReservationResponse(
        Long reservationId,
        Long amount,
        String orderName
) {

    public static PaymentReservationResponse from(PaymentReservationResult result) {
        return new PaymentReservationResponse(
                result.reservationId(),
                result.amount(),
                result.orderName()
        );
    }
}

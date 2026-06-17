package roomescape.payment.application.port.in.dto;

public record PaymentReservationResult(
        Long reservationId,
        Long amount,
        String orderName
) {
}

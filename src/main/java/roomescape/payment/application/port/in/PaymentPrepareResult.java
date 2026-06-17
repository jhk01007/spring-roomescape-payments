package roomescape.payment.application.port.in;

public record PaymentPrepareResult(
        String orderId,
        Long reservationId
) {
}

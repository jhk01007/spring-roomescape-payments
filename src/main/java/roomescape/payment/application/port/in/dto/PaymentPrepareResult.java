package roomescape.payment.application.port.in.dto;

public record PaymentPrepareResult(
        String orderId,
        Long reservationId,
        Long amount,
        String orderName
) {
}

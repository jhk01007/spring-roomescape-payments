package roomescape.payment.application.port.out;

public interface PaymentSessionRepository {

    void save(String orderId, Long reservationId, Long amount);
}

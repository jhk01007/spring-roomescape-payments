package roomescape.payment.application.port.in;

public interface PaymentPrepareUseCase {
    PaymentPrepareResult prepare(PaymentPrepareCommand command);
}

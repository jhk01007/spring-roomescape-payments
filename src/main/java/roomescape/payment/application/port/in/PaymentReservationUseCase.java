package roomescape.payment.application.port.in;

import roomescape.payment.application.port.in.dto.PaymentReservationCommand;
import roomescape.payment.application.port.in.dto.PaymentReservationResult;

public interface PaymentReservationUseCase {

    PaymentReservationResult create(PaymentReservationCommand command);
}

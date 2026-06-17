package roomescape.payment.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import roomescape.payment.application.port.in.dto.PaymentPrepareCommand;

public record PaymentPrepareRequest(
        @NotNull(message = "예약 id는 비어 있을 수 없습니다.")
        Long reservationId
) {

    public PaymentPrepareCommand toCommand() {
        return new PaymentPrepareCommand(reservationId);
    }
}

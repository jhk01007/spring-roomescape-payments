package roomescape.payment.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import roomescape.payment.application.port.in.PaymentPrepareCommand;

import java.time.LocalDate;

public record PaymentPrepareRequest(
        @NotBlank(message = "예약자 이름은 비어 있을 수 없습니다.")
        String guestName,
        @NotNull(message = "예약 날짜는 비어 있을 수 없습니다.")
        LocalDate date,
        @NotNull(message = "예약 시간은 비어 있을 수 없습니다.")
        Long timeId,
        @NotNull(message = "테마는 비어 있을 수 없습니다.")
        Long themeId,
        @NotNull(message = "결제 금액은 비어 있을 수 없습니다.")
        @Positive(message = "결제 금액은 0보다 커야 합니다.")
        Long amount
) {

    public PaymentPrepareCommand toCommand() {
        return new PaymentPrepareCommand(guestName, date, timeId, themeId, amount);
    }
}

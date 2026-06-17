package roomescape.payment.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import roomescape.payment.application.port.in.dto.PaymentReservationCommand;

import java.time.LocalDate;

public record PaymentReservationRequest(
        @NotBlank(message = "예약자 이름은 비어 있을 수 없습니다.")
        String guestName,
        @NotNull(message = "예약 날짜는 비어 있을 수 없습니다.")
        LocalDate date,
        @NotNull(message = "예약 시간은 비어 있을 수 없습니다.")
        Long timeId,
        @NotNull(message = "테마는 비어 있을 수 없습니다.")
        Long themeId
) {

    public PaymentReservationCommand toCommand() {
        return new PaymentReservationCommand(guestName, date, timeId, themeId);
    }
}

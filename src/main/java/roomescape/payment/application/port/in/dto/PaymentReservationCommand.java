package roomescape.payment.application.port.in.dto;

import java.time.LocalDate;

public record PaymentReservationCommand(
        String guestName,
        LocalDate date,
        Long timeId,
        Long themeId
) {
}

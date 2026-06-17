package roomescape.payment.application.port.in;

import java.time.LocalDate;

public record PaymentPrepareCommand(
        String guestName,
        LocalDate date,
        Long timeId,
        Long themeId,
        Long amount
) {
}

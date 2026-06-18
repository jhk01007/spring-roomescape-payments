package roomescape.reservation.service.dto;


import roomescape.reservation.domain.ReservationStatus;
import roomescape.reservation.repository.dto.ReservationWaitingDto;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.theme.domain.Theme;

import java.time.LocalDate;

public record ReservationWaitingResult(
        Long id,
        String guestName,
        LocalDate date,
        ReservationTime time,
        Theme theme,
        ReservationStatus reservationStatus,
        long waitNumber
) {
    public static ReservationWaitingResult from(ReservationWaitingDto reservationWaitingDto) {
        return new ReservationWaitingResult(
                reservationWaitingDto.id(),
                reservationWaitingDto.guestName(),
                reservationWaitingDto.date(),
                reservationWaitingDto.time(),
                reservationWaitingDto.theme(),
                reservationWaitingDto.reservationStatus(),
                reservationWaitingDto.waitNumber()
        );
    }
}

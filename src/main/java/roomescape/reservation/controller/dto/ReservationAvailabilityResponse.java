package roomescape.reservation.controller.dto;

import roomescape.reservation.service.dto.ReservationSlotAvailability;

public record ReservationAvailabilityResponse(
        String availability
) {

    public static ReservationAvailabilityResponse from(ReservationSlotAvailability availability) {
        return new ReservationAvailabilityResponse(availability.name());
    }
}

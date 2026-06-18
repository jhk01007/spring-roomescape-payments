package roomescape.reservation.domain;

import roomescape.common.exception.DomainException;

import java.util.Arrays;

import static roomescape.reservation.exception.ReservationErrorCode.INVALID_STATUS;

public enum ReservationStatus {
    WAITING, CONFIRMED, PENDING, REQUIRES_CHECK, CANCELED;

    public static ReservationStatus from(String status) {
        return Arrays.stream(ReservationStatus.values())
                .filter(s -> s.toString().equals(status))
                .findFirst()
                .orElseThrow(() -> new DomainException(INVALID_STATUS));
    }
}

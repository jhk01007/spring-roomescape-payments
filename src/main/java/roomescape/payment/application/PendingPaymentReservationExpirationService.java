package roomescape.payment.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PendingPaymentReservationExpirationService {

    private static final Duration EXPIRATION_THRESHOLD = Duration.ofMinutes(10);

    private final ReservationRepository reservationRepository;
    private final Clock clock;

    @Transactional
    public int expire() {
        LocalDateTime lastModifiedAtThreshold = LocalDateTime.now(clock)
                .minus(EXPIRATION_THRESHOLD);
        List<Reservation> expiredReservations =
                reservationRepository.findAllPendingLastModifiedAtBeforeOrEqual(lastModifiedAtThreshold);

        expiredReservations.forEach(Reservation::cancel);
        return expiredReservations.size();
    }
}

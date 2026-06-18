package roomescape.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PendingPaymentReservationExpirationService {

    private final ReservationRepository reservationRepository;
    private final Clock clock;

    @Transactional
    public int expire() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Reservation> expiredReservations =
                reservationRepository.findAllPendingPaymentExpiresAtBeforeOrEqual(now);

        expiredReservations.forEach(Reservation::cancel);
        return expiredReservations.size();
    }
}

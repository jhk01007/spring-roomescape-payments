package roomescape.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "payment.pending-reservation-expiration.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PendingPaymentReservationExpirationScheduler {

    private final PendingPaymentReservationExpirationService expirationService;

    @Scheduled(
            fixedDelayString = "${payment.pending-reservation-expiration.fixed-delay-ms:60000}",
            initialDelayString = "${payment.pending-reservation-expiration.initial-delay-ms:60000}"
    )
    public void expirePendingReservations() {
        expirationService.expire();
    }
}

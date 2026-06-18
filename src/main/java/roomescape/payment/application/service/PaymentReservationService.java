package roomescape.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.DomainException;
import roomescape.payment.application.port.in.dto.PaymentReservationCommand;
import roomescape.payment.application.port.in.dto.PaymentReservationResult;
import roomescape.payment.application.port.in.PaymentReservationUseCase;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;
import roomescape.reservation.service.validator.ReservationValidator;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.reservationtime.domain.ReservationTimeRepository;
import roomescape.theme.domain.Theme;
import roomescape.theme.domain.ThemeRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import static roomescape.reservation.domain.ReservationStatus.PENDING;
import static roomescape.reservationtime.exeption.ReservationTimeErrorCode.RESERVATION_TIME_NOT_FOUND;
import static roomescape.theme.exception.ThemeErrorCode.THEME_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PaymentReservationService implements PaymentReservationUseCase {

    private static final Duration PAYMENT_EXPIRATION = Duration.ofMinutes(10);

    private final ReservationRepository reservationRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final ThemeRepository themeRepository;
    private final ReservationValidator reservationValidator;
    private final Clock clock;

    @Override
    @Transactional
    public PaymentReservationResult create(PaymentReservationCommand command) {
        ReservationTime time = getReservationTime(command.timeId());
        Theme theme = getTheme(command.themeId());

        LocalDateTime now = LocalDateTime.now(clock);
        Reservation reservation = Reservation.create(
                command.guestName(),
                command.date(),
                time,
                theme,
                PENDING,
                now,
                now.plus(PAYMENT_EXPIRATION)
        );
        reservationValidator.validateCreatePending(reservation);

        Reservation saved = reservationRepository.save(reservation);

        return new PaymentReservationResult(saved.getId(), theme.getPrice(), theme.getName());
    }

    private ReservationTime getReservationTime(Long timeId) {
        return reservationTimeRepository.findById(timeId)
                .orElseThrow(() -> new DomainException(RESERVATION_TIME_NOT_FOUND));
    }

    private Theme getTheme(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new DomainException(THEME_NOT_FOUND));
    }
}

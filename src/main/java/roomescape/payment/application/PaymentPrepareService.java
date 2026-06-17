package roomescape.payment.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.DomainException;
import roomescape.payment.application.port.in.PaymentPrepareCommand;
import roomescape.payment.application.port.in.PaymentPrepareResult;
import roomescape.payment.application.port.in.PaymentPrepareUseCase;
import roomescape.payment.application.port.out.PaymentSessionRepository;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;
import roomescape.reservation.service.validator.ReservationValidator;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.reservationtime.domain.ReservationTimeRepository;
import roomescape.theme.domain.Theme;
import roomescape.theme.domain.ThemeRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import static roomescape.reservation.domain.Status.PENDING;
import static roomescape.reservationtime.exeption.ReservationTimeErrorCode.RESERVATION_TIME_NOT_FOUND;
import static roomescape.theme.exception.ThemeErrorCode.THEME_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PaymentPrepareService implements PaymentPrepareUseCase {

    private final ReservationRepository reservationRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final ThemeRepository themeRepository;
    private final ReservationValidator reservationValidator;
    private final PaymentSessionRepository paymentSessionRepository;
    private final Clock clock;

    @Override
    @Transactional
    public PaymentPrepareResult prepare(PaymentPrepareCommand command) {
        ReservationTime time = getReservationTime(command.timeId());
        Theme theme = getTheme(command.themeId());
        Long amount = theme.getPrice();
        String orderName = theme.getName();

        Reservation reservation = Reservation.create(
                command.guestName(),
                command.date(),
                time,
                theme,
                PENDING,
                LocalDateTime.now(clock)
        );
        reservationValidator.validateCreatePending(reservation);

        Reservation saved = reservationRepository.save(reservation);
        String orderId = createOrderId();
        paymentSessionRepository.save(orderId, saved.getId(), amount);

        return new PaymentPrepareResult(orderId, saved.getId(), amount, orderName);
    }

    private ReservationTime getReservationTime(Long timeId) {
        return reservationTimeRepository.findById(timeId)
                .orElseThrow(() -> new DomainException(RESERVATION_TIME_NOT_FOUND));
    }

    private Theme getTheme(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new DomainException(THEME_NOT_FOUND));
    }

    private String createOrderId() {
        return "order-" + UUID.randomUUID().toString().replace("-", "");
    }
}

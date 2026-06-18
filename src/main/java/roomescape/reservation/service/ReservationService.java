package roomescape.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.dto.PageResult;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationStatus;
import roomescape.reservation.service.dto.ReservationSlotAvailability;
import roomescape.reservation.service.dto.ReservationWaitingResult;
import roomescape.reservation.service.validator.ReservationValidator;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.reservationtime.domain.ReservationTimeRepository;
import roomescape.theme.domain.Theme;
import roomescape.common.exception.DomainException;
import roomescape.reservation.domain.ReservationRepository;
import roomescape.theme.domain.ThemeRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static roomescape.reservation.domain.ReservationStatus.CONFIRMED;
import static roomescape.reservation.domain.ReservationStatus.PENDING;
import static roomescape.reservation.domain.ReservationStatus.WAITING;
import static roomescape.reservation.exception.ReservationErrorCode.*;
import static roomescape.reservation.service.dto.ReservationSlotAvailability.*;
import static roomescape.reservationtime.exeption.ReservationTimeErrorCode.*;
import static roomescape.theme.exception.ThemeErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {
    private static final Duration PROMOTED_PAYMENT_EXPIRATION = Duration.ofHours(1);

    private final ReservationRepository reservationRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final ThemeRepository themeRepository;

    private final ReservationValidator reservationValidator;
    private final Clock clock;

    @Transactional
    public ReservationWaitingResult createWaiting(String guestName, LocalDate date, Long timeId, Long themeId) {
        ReservationTime time = getReservationTime(timeId);
        Theme theme = getTheme(themeId);

        Reservation reservation = Reservation.create(guestName, date, time, theme, WAITING, LocalDateTime.now(clock));

        reservationValidator.validateCreate(reservation);
        validateWaitingAvailable(date, timeId, themeId);

        Reservation saved = reservationRepository.save(reservation);

        return ReservationWaitingResult.from(reservationRepository.findWaitingById(saved.getId())
                .orElseThrow(() -> new DomainException(RESERVATION_NOT_FOUND)));
    }

    public PageResult<Reservation> findAllReservations(int page, int size) {
        return reservationRepository.findAllByStatusCanceledNot(page, size);
    }

    public List<ReservationWaitingResult> findByGuestName(String guestName) {
        return reservationRepository.findWaitingAllByGuestName(guestName).stream()
                .map(ReservationWaitingResult::from)
                .toList();
    }

    public ReservationSlotAvailability findSlotAvailability(LocalDate date, Long timeId, Long themeId) {
        ReservationTime time = getReservationTime(timeId);
        getTheme(themeId);

        if (LocalDateTime.of(date, time.getStartAt()).isBefore(LocalDateTime.now(clock))) {
            return UNAVAILABLE;
        }
        if (reservationRepository.existsBySlotAndStatusConfirmedOrPending(date, timeId, themeId)) {
            return ReservationSlotAvailability.WAITING;
        }
        if (reservationRepository.existsBySlot(date, timeId, themeId)) {
            return UNAVAILABLE;
        }
        return AVAILABLE;
    }

    @Transactional
    public void editDateTime(Long reservationId, LocalDate changedDate, Long changedTimeId, String requestGuestName) {
        Reservation beforeReservation = getReservation(reservationId);
        reservationValidator.validateBeforeEdit(beforeReservation, changedDate, changedTimeId, requestGuestName);

        ReservationTime changedTime = getReservationTime(changedTimeId);

        ReservationStatus afterReservationStatus = determineState(beforeReservation, changedDate, changedTimeId);
        LocalDateTime now = LocalDateTime.now(clock);
        Reservation changedReservation = beforeReservation.changeDateTimeAndStatus(
                changedDate, changedTime, afterReservationStatus, now);

        validateEdit(beforeReservation, changedReservation);

        updateTopWaitingConfirmed(beforeReservation);
        beforeReservation.updateDateTimeAndStatus(changedDate, changedTime, afterReservationStatus, now);
    }

    private void updateTopWaitingConfirmed(Reservation reservation) {
        if (reservation.isConfirmed()) {
            Optional<Reservation> topWaiting = reservationRepository.findBySlotAndStatusWaitingAndWaitingNumberIsOne(
                    reservation.getDate(),
                    reservation.getTimeId(),
                    reservation.getThemeId());

            if (topWaiting.isPresent()) {
                Reservation top = topWaiting.get();
                top.updateStatus(CONFIRMED);
            }
        }
    }

    private void updateTopWaitingPending(Reservation reservation) {
        if (reservation.isConfirmed()) {
            Optional<Reservation> topWaiting = reservationRepository.findBySlotAndStatusWaitingAndWaitingNumberIsOne(
                    reservation.getDate(),
                    reservation.getTimeId(),
                    reservation.getThemeId());

            if (topWaiting.isPresent()) {
                Reservation top = topWaiting.get();
                LocalDateTime now = LocalDateTime.now(clock);
                top.waitForPayment(now, now.plus(PROMOTED_PAYMENT_EXPIRATION));
            }
        }
    }

    @Transactional
    public void cancel(Long id) {
        Reservation reservation = getReservation(id);
        reservationValidator.validateCancel(reservation);
        updateTopWaitingPending(reservation);
        reservation.cancel();
    }

    @Transactional
    public void cancelMine(Long id, String guestName) {
        Reservation reservation = getReservation(id);
        reservationValidator.validateCancelMine(reservation, guestName);
        updateTopWaitingPending(reservation);
        reservation.cancel();
    }

    private Theme getTheme(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new DomainException(THEME_NOT_FOUND));
    }

    private Reservation getReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new DomainException(RESERVATION_NOT_FOUND));
    }

    private ReservationTime getReservationTime(Long timeId) {
        return reservationTimeRepository.findById(timeId)
                .orElseThrow(() -> new DomainException(RESERVATION_TIME_NOT_FOUND));
    }

    private void validateWaitingAvailable(LocalDate date, Long timeId, Long themeId) {
        if (!reservationRepository.existsBySlotAndStatusConfirmedOrPending(date, timeId, themeId)) {
            throw new DomainException(RESERVATION_WAITING_REQUIRES_OCCUPIED_SLOT);
        }
    }

    private void validateEdit(Reservation beforeReservation, Reservation changedReservation) {
        if (beforeReservation.isPending()) {
            reservationValidator.validateCreatePending(changedReservation);
            return;
        }
        reservationValidator.validateEdit(changedReservation);
    }

    private ReservationStatus determineState(Reservation beforeReservation, LocalDate date, Long timeId) {
        if (beforeReservation.isPending()) {
            return PENDING;
        }

        Long themeId = beforeReservation.getTheme().getId();
        if (!reservationRepository.existsBySlotAndStatusConfirmedOrPending(date, timeId, themeId)) {
            return CONFIRMED;
        }
        return WAITING;
    }

}

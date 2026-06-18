package roomescape.test_config.fixture;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationStatus;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.theme.domain.Theme;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@Transactional
public class SQLFixtureGenerator {

    private final EntityManager entityManager;

    public SQLFixtureGenerator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Reservation insertReservation(
            String guestName, LocalDate date, ReservationTime time, Theme theme, ReservationStatus reservationStatus) {
        LocalDateTime now = LocalDateTime.now();

        return insertReservation(guestName, date, time, theme, reservationStatus, now);
    }

    public Reservation insertReservation(
            String guestName,
            LocalDate date,
            ReservationTime time,
            Theme theme,
            ReservationStatus reservationStatus,
            LocalDateTime lastModifiedAt
    ) {
        LocalDateTime paymentExpiresAt = null;
        if (ReservationStatus.PENDING.equals(reservationStatus)) {
            paymentExpiresAt = lastModifiedAt.plusMinutes(10);
        }

        return insertReservation(guestName, date, time, theme, reservationStatus, lastModifiedAt, paymentExpiresAt);
    }

    public Reservation insertReservation(
            String guestName,
            LocalDate date,
            ReservationTime time,
            Theme theme,
            ReservationStatus reservationStatus,
            LocalDateTime lastModifiedAt,
            LocalDateTime paymentExpiresAt
    ) {
        ReservationTime attachedTime = entityManager.getReference(ReservationTime.class, time.getId());
        Theme attachedTheme = entityManager.getReference(Theme.class, theme.getId());
        Reservation reservation = Reservation.create(
                guestName,
                date,
                attachedTime,
                attachedTheme,
                reservationStatus,
                lastModifiedAt,
                paymentExpiresAt
        );
        entityManager.persist(reservation);
        if (ReservationStatus.CANCELED.equals(reservationStatus)) {
            flush();
            reservation.cancel();
        }
        flush();
        return reservation;
    }

    public Reservation insertDeletedReservation(String guestName, LocalDate date, ReservationTime time, Theme theme) {
        Reservation reservation = insertReservation(guestName, date, time, theme, ReservationStatus.CONFIRMED);
        reservation.cancel();
        flush();
        return reservation;
    }

    public ReservationTime insertReservationTime(LocalTime startAt) {
        ReservationTime reservationTime = ReservationTime.create(startAt);
        entityManager.persist(reservationTime);
        flush();
        return reservationTime;
    }

    public ReservationTime insertDeletedReservationTime(LocalTime startAt) {
        ReservationTime reservationTime = insertReservationTime(startAt);
        reservationTime.cancel(LocalDateTime.now());
        flush();
        return reservationTime;
    }

    public Theme insertTheme(String name, String description, String thumbnail) {
        return insertTheme(name, description, thumbnail, Theme.DEFAULT_PRICE);
    }

    public Theme insertTheme(String name, String description, String thumbnail, Long price) {
        Theme theme = Theme.create(name, description, thumbnail, price);
        entityManager.persist(theme);
        flush();
        return theme;
    }

    public Theme insertDeletedTheme(String name, String description, String thumbnail) {
        Theme theme = insertTheme(name, description, thumbnail);
        theme.cancel(LocalDateTime.now());
        flush();
        return theme;
    }

    private void flush() {
        entityManager.flush();
    }
}

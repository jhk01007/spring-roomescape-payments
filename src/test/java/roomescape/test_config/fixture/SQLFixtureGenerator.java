package roomescape.test_config.fixture;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.Status;
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
            String guestName, LocalDate date, ReservationTime time, Theme theme, Status status) {
        LocalDateTime now = LocalDateTime.now();

        return insertReservation(guestName, date, time, theme, status, now);
    }

    public Reservation insertReservation(
            String guestName,
            LocalDate date,
            ReservationTime time,
            Theme theme,
            Status status,
            LocalDateTime lastModifiedAt
    ) {
        ReservationTime attachedTime = entityManager.getReference(ReservationTime.class, time.getId());
        Theme attachedTheme = entityManager.getReference(Theme.class, theme.getId());
        Reservation reservation = Reservation.create(guestName, date, attachedTime, attachedTheme, status, lastModifiedAt);
        entityManager.persist(reservation);
        if (Status.CANCELED.equals(status)) {
            flush();
            reservation.cancel();
        }
        flush();
        return reservation;
    }

    public Reservation insertDeletedReservation(String guestName, LocalDate date, ReservationTime time, Theme theme) {
        Reservation reservation = insertReservation(guestName, date, time, theme, Status.CONFIRMED);
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
        Theme theme = Theme.create(name, description, thumbnail);
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

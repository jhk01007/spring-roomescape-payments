package roomescape.reservation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import roomescape.common.exception.DomainException;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.theme.domain.Theme;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import static roomescape.common.domain.DomainPreconditions.require;
import static roomescape.common.domain.DomainPreconditions.requireNonBlank;
import static roomescape.common.domain.DomainPreconditions.requireNonNull;
import static roomescape.reservation.domain.ReservationStatus.*;
import static roomescape.reservation.domain.ReservationStatus.CANCELED;
import static roomescape.reservation.exception.ReservationErrorCode.*;
import static roomescape.reservationtime.exeption.ReservationTimeErrorCode.*;
import static roomescape.theme.exception.ThemeErrorCode.*;

@Getter
@Entity
@Table(name = "reservation")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guest_name", nullable = false)
    private String guestName;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(optional = false)
    @JoinColumn(name = "time_id", nullable = false)
    private ReservationTime time;

    @ManyToOne(optional = false)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus reservationStatus;

    @Column(name = "cancel_token", nullable = false)
    private Long cancelToken;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    @Column(name = "payment_expires_at")
    private LocalDateTime paymentExpiresAt;

    protected Reservation() {
    }

    private Reservation(
            Long id, String guestName, LocalDate date, ReservationTime time, Theme theme, ReservationStatus reservationStatus, LocalDateTime lastModifiedAt) {
        this(id, guestName, date, time, theme, reservationStatus, lastModifiedAt, null);
    }

    private Reservation(
            Long id,
            String guestName,
            LocalDate date,
            ReservationTime time,
            Theme theme,
            ReservationStatus reservationStatus,
            LocalDateTime lastModifiedAt,
            LocalDateTime paymentExpiresAt
    ) {
        validateReservation(guestName, date, time, theme, lastModifiedAt);
        this.id = id;
        this.guestName = guestName;
        this.date = date;
        this.time = time;
        this.theme = theme;
        this.reservationStatus = reservationStatus;
        this.cancelToken = 0L;
        this.lastModifiedAt = lastModifiedAt;
        this.paymentExpiresAt = paymentExpiresAtFor(reservationStatus, paymentExpiresAt);
    }

    public static Reservation create(
            String guestName, LocalDate date, ReservationTime time, Theme theme, ReservationStatus reservationStatus, LocalDateTime lastModifiedAt) {
        return new Reservation(null, guestName, date, time, theme, reservationStatus, lastModifiedAt);
    }

    public static Reservation create(
            String guestName,
            LocalDate date,
            ReservationTime time,
            Theme theme,
            ReservationStatus reservationStatus,
            LocalDateTime lastModifiedAt,
            LocalDateTime paymentExpiresAt
    ) {
        return new Reservation(null, guestName, date, time, theme, reservationStatus, lastModifiedAt, paymentExpiresAt);
    }

    public static Reservation of(
            long id,
            String guestName,
            LocalDate date,
            ReservationTime time,
            Theme theme,
            ReservationStatus reservationStatus,
            LocalDateTime lastModifiedAt
    ) {
        return new Reservation(id, guestName, date, time, theme, reservationStatus, lastModifiedAt);
    }

    public static Reservation of(
            long id,
            String guestName,
            LocalDate date,
            ReservationTime time,
            Theme theme,
            ReservationStatus reservationStatus,
            LocalDateTime lastModifiedAt,
            LocalDateTime paymentExpiresAt
    ) {
        return new Reservation(id, guestName, date, time, theme, reservationStatus, lastModifiedAt, paymentExpiresAt);
    }

    public static Reservation clone(Reservation reservation) {
        return new Reservation(
                reservation.id,
                reservation.guestName,
                reservation.date,
                reservation.time,
                reservation.theme,
                reservation.reservationStatus,
                reservation.lastModifiedAt,
                reservation.paymentExpiresAt);
    }

    public Reservation withId(long id) {
        require(this.id == null, new DomainException(RESERVATION_ALREADY_HAS_ID));
        return of(id, guestName, date, time, theme, reservationStatus, lastModifiedAt, paymentExpiresAt);
    }

    private void validateReservation(String guestName, LocalDate date, ReservationTime time, Theme theme, LocalDateTime lastModifiedAt) {
        requireNonBlank(guestName, new DomainException(INVALID_RESERVATION_GUEST_NAME));
        requireNonNull(date, new DomainException(INVALID_RESERVATION_DATE));
        requireNonNull(time, new DomainException(INVALID_RESERVATION_TIME));
        requireNonNull(theme, new DomainException(INVALID_THEME));
        requireNonNull(lastModifiedAt, new DomainException(INVALID_LAST_MODIFIED_AT));
    }

    private LocalDateTime paymentExpiresAtFor(ReservationStatus reservationStatus, LocalDateTime paymentExpiresAt) {
        if (PENDING.equals(reservationStatus)) {
            return paymentExpiresAt;
        }
        return null;
    }

    public Long getThemeId() {
        return theme.getId();
    }

    public Long getTimeId() {
        return time.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    public boolean isPassed(LocalDateTime now) {
        return LocalDateTime.of(date, time.getStartAt())
                .isBefore(now);
    }

    public boolean isSameGuest(String guestName) {
        return Objects.equals(this.guestName, guestName);
    }

    public Reservation changeDateTimeAndStatus(
            LocalDate changedDate, ReservationTime changedTime, ReservationStatus reservationStatus, LocalDateTime lastModifiedAt) {
        return new Reservation(id, guestName, changedDate, changedTime, theme, reservationStatus, lastModifiedAt, paymentExpiresAt);
    }

    public Reservation changeStatus(ReservationStatus reservationStatus) {
        return new Reservation(id, guestName, date, time, theme, reservationStatus, lastModifiedAt, paymentExpiresAt);
    }

    public void updateDateTimeAndStatus(
            LocalDate changedDate, ReservationTime changedTime, ReservationStatus reservationStatus, LocalDateTime lastModifiedAt) {
        validateReservation(guestName, changedDate, changedTime, theme, lastModifiedAt);
        this.date = changedDate;
        this.time = changedTime;
        this.reservationStatus = reservationStatus;
        this.lastModifiedAt = lastModifiedAt;
        this.paymentExpiresAt = paymentExpiresAtFor(reservationStatus, paymentExpiresAt);
    }

    public void updateStatus(ReservationStatus reservationStatus) {
        this.reservationStatus = reservationStatus;
        this.paymentExpiresAt = paymentExpiresAtFor(reservationStatus, paymentExpiresAt);
    }

    public void waitForPayment(LocalDateTime lastModifiedAt, LocalDateTime paymentExpiresAt) {
        requireNonNull(lastModifiedAt, new DomainException(INVALID_LAST_MODIFIED_AT));
        requireNonNull(paymentExpiresAt, new DomainException(INVALID_PAYMENT_EXPIRES_AT));
        this.reservationStatus = PENDING;
        this.lastModifiedAt = lastModifiedAt;
        this.paymentExpiresAt = paymentExpiresAt;
    }

    public void confirm() {
        this.reservationStatus = CONFIRMED;
        this.paymentExpiresAt = null;
    }

    public void requirePaymentCheck() {
        this.reservationStatus = REQUIRES_CHECK;
        this.paymentExpiresAt = null;
    }

    public void cancel() {
        this.reservationStatus = CANCELED;
        this.cancelToken = id;
        this.paymentExpiresAt = null;
    }

    public boolean isConfirmed() {
        return CONFIRMED.equals(reservationStatus);
    }

    public boolean isPending() {
        return PENDING.equals(reservationStatus);
    }

    public boolean isRequiresCheck() {
        return REQUIRES_CHECK.equals(reservationStatus);
    }

    public boolean isCanceled() {
        return CANCELED.equals(reservationStatus);
    }

    public boolean isPaymentExpired(LocalDateTime now) {
        return isPending() && paymentExpiresAt != null && !paymentExpiresAt.isAfter(now);
    }

    public boolean isSameDateTime(LocalDate date, Long timeId) {
        return this.date.isEqual(date) && Objects.equals(this.time.getId(), timeId);
    }
}

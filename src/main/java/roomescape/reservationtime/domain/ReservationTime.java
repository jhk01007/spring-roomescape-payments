package roomescape.reservationtime.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import roomescape.common.exception.DomainException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import static roomescape.common.domain.DomainPreconditions.require;
import static roomescape.common.domain.DomainPreconditions.requireNonNull;
import static roomescape.reservationtime.exeption.ReservationTimeErrorCode.*;

@Getter
@Entity
@Table(name = "reservation_time")
public class ReservationTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_at", nullable = false)
    private LocalTime startAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "delete_token", nullable = false)
    private Long deleteToken;

    protected ReservationTime() {
    }

    private ReservationTime(Long id, LocalTime startAt, LocalDateTime deletedAt, Long deleteToken) {
        validateStartAt(startAt);
        this.id = id;
        this.startAt = startAt;
        this.deletedAt = deletedAt;
        this.deleteToken = deleteToken;
    }

    public static ReservationTime create(LocalTime startAt) {
        return new ReservationTime(null, startAt, null, 0L);
    }

    public static ReservationTime of(long id, LocalTime startAt) {
        return of(id, startAt, null);
    }

    public static ReservationTime of(long id, LocalTime startAt, LocalDateTime deletedAt) {
        return new ReservationTime(id, startAt, deletedAt, 0L);
    }

    public ReservationTime withId(long id) {
        require(this.id == null, new DomainException(RESERVATION_TIME_ALREADY_HAS_ID));
        return of(id, startAt, deletedAt);
    }

    public void cancel(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
        this.deleteToken = id;
    }

    private void validateStartAt(LocalTime startAt) {
        requireNonNull(startAt, new DomainException(INVALID_RESERVATION_TIME));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReservationTime that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}

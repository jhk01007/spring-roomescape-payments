package roomescape.reservation.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import roomescape.common.dto.PageResult;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;
import roomescape.reservation.domain.Status;
import roomescape.reservation.repository.dto.ReservationWaitingDto;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.theme.domain.Theme;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class ReservationRepositoryImpl implements ReservationRepository {

    private static final String WAITING_SELECT = """
            SELECT
                r.id AS reservation_id,
                r.guest_name,
                r.date,
                r.status AS status,
                r.last_modified_at AS last_modified_at,
                t.id AS time_id,
                t.start_at,
                t.deleted_at AS time_deleted_at,
                th.id AS theme_id,
                th.name AS theme_name,
                th.description AS theme_description,
                th.thumbnail AS theme_thumbnail,
                th.price AS theme_price,
                th.deleted_at AS theme_deleted_at,
                ROW_NUMBER() OVER (
                    PARTITION BY r.date, t.id, th.id, r.status
                    ORDER BY r.last_modified_at
                ) AS wait_number
            FROM reservation r
            INNER JOIN reservation_time t
                ON r.time_id = t.id
            INNER JOIN theme th
                ON r.theme_id = th.id
            """;

    private final JpaReservationRepository jpaReservationRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<Reservation> findById(Long id) {
        return jpaReservationRepository.findById(id);
    }

    @Override
    public Optional<ReservationWaitingDto> findWaitingById(Long id) {
        return findWaitingRows("""
                SELECT *
                FROM (
                %s
                ) x
                WHERE x.reservation_id = :id
                """.formatted(WAITING_SELECT))
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .map(this::toReservationWaitingDto);
    }

    @Override
    public PageResult<Reservation> findAllByStatusCanceledNot(int page, int size) {
        List<Reservation> reservations = entityManager.createQuery("""
                        SELECT reservation
                        FROM Reservation reservation
                        JOIN FETCH reservation.time time
                        JOIN FETCH reservation.theme
                        WHERE reservation.status != :status
                        ORDER BY reservation.date, time.startAt
                        """, Reservation.class)
                .setParameter("status", Status.CANCELED)
                .setFirstResult((page - 1) * size)
                .setMaxResults(size)
                .getResultList();

        Long count = entityManager.createQuery("""
                        SELECT COUNT(reservation)
                        FROM Reservation reservation
                        WHERE reservation.status != :status
                        """, Long.class)
                .setParameter("status", Status.CANCELED)
                .getSingleResult();

        return PageResult.of(reservations, page, size, count);
    }

    @Override
    public List<ReservationWaitingDto> findWaitingAllByGuestName(String guestName) {
        return findWaitingRows("""
                SELECT *
                FROM (
                %s
                ) x
                WHERE x.guest_name = :guestName
                """.formatted(WAITING_SELECT))
                .setParameter("guestName", guestName)
                .getResultStream()
                .map(this::toReservationWaitingDto)
                .toList();
    }

    @Override
    public Optional<Reservation> findBySlotAndStatusWaitingAndWaitingNumberIsOne(
            LocalDate date, Long timeId, Long themeId) {
        Optional<?> row = findWaitingRows("""
                SELECT *
                FROM (
                %s
                ) x
                WHERE x.date = :date
                  AND x.time_id = :timeId
                  AND x.theme_id = :themeId
                  AND x.status = 'WAITING'
                  AND x.wait_number = 1
                """.formatted(WAITING_SELECT))
                .setParameter("date", Date.valueOf(date))
                .setParameter("timeId", timeId)
                .setParameter("themeId", themeId)
                .getResultStream()
                .findFirst();

        return row
                .map(value -> toLong(((Object[]) value)[0]))
                .flatMap(jpaReservationRepository::findById);
    }

    @Override
    public Reservation save(Reservation reservation) {
        ReservationTime time = entityManager.getReference(ReservationTime.class, reservation.getTimeId());
        Theme theme = entityManager.getReference(Theme.class, reservation.getThemeId());
        Reservation attachedReservation = Reservation.create(
                reservation.getGuestName(),
                reservation.getDate(),
                time,
                theme,
                reservation.getStatus(),
                reservation.getLastModifiedAt()
        );

        return jpaReservationRepository.save(attachedReservation);
    }

    @Override
    public boolean existsBySlotAndGuestNameExceptCanceled(
            LocalDate date, Long timeId, Long themeId, String guestName) {
        Long count = entityManager.createQuery("""
                        SELECT COUNT(reservation)
                        FROM Reservation reservation
                        WHERE reservation.date = :date
                          AND reservation.time.id = :timeId
                          AND reservation.theme.id = :themeId
                          AND reservation.guestName = :guestName
                          AND reservation.status != :status
                        """, Long.class)
                .setParameter("date", date)
                .setParameter("timeId", timeId)
                .setParameter("themeId", themeId)
                .setParameter("guestName", guestName)
                .setParameter("status", Status.CANCELED)
                .getSingleResult();

        return count > 0;
    }

    @Override
    public boolean existsBySlotAndStatusConfirmedOrPending(LocalDate date, Long timeId, Long themeId) {
        Long count = entityManager.createQuery("""
                        SELECT COUNT(reservation)
                        FROM Reservation reservation
                        WHERE reservation.date = :date
                          AND reservation.time.id = :timeId
                          AND reservation.theme.id = :themeId
                          AND reservation.status IN :statuses
                        """, Long.class)
                .setParameter("date", date)
                .setParameter("timeId", timeId)
                .setParameter("themeId", themeId)
                .setParameter("statuses", List.of(Status.CONFIRMED, Status.PENDING))
                .getSingleResult();

        return count > 0;
    }

    @Override
    public boolean existByTimeId(Long timeId) {
        Long count = entityManager.createQuery("""
                        SELECT COUNT(reservation)
                        FROM Reservation reservation
                        WHERE reservation.time.id = :timeId
                          AND reservation.status != :status
                        """, Long.class)
                .setParameter("timeId", timeId)
                .setParameter("status", Status.CANCELED)
                .getSingleResult();

        return count > 0;
    }

    @Override
    public boolean existByThemeId(Long themeId) {
        Long count = entityManager.createQuery("""
                        SELECT COUNT(reservation)
                        FROM Reservation reservation
                        WHERE reservation.theme.id = :themeId
                          AND reservation.status != :status
                        """, Long.class)
                .setParameter("themeId", themeId)
                .setParameter("status", Status.CANCELED)
                .getSingleResult();

        return count > 0;
    }

    @Override
    public boolean existsBySlot(LocalDate date, Long timeId, Long themeId) {
        return jpaReservationRepository.existsByDateAndTimeIdAndThemeId(date, timeId, themeId, Status.CANCELED);
    }

    private Query findWaitingRows(String sql) {
        return entityManager.createNativeQuery(sql);
    }

    private ReservationWaitingDto toReservationWaitingDto(Object row) {
        Object[] columns = (Object[]) row;
        return ReservationWaitingDto.from(toReservation(columns), toLong(columns[14]));
    }

    private Reservation toReservation(Object row) {
        Object[] columns = (Object[]) row;
        ReservationTime time = ReservationTime.of(
                toLong(columns[5]),
                toLocalTime(columns[6]),
                toLocalDateTime(columns[7])
        );
        Theme theme = Theme.of(
                toLong(columns[8]),
                (String) columns[9],
                (String) columns[10],
                (String) columns[11],
                toLong(columns[12]),
                toLocalDateTime(columns[13])
        );
        return Reservation.of(
                toLong(columns[0]),
                (String) columns[1],
                toLocalDate(columns[2]),
                time,
                theme,
                toStatus(columns[3]),
                toLocalDateTime(columns[4])
        );
    }

    private Long toLong(Object value) {
        return ((Number) value).longValue();
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    private LocalTime toLocalTime(Object value) {
        if (value instanceof LocalTime localTime) {
            return localTime;
        }
        if (value instanceof Time time) {
            return time.toLocalTime();
        }
        return LocalTime.parse(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return Timestamp.valueOf(value.toString()).toLocalDateTime();
    }

    private Status toStatus(Object value) {
        if (value instanceof Status status) {
            return status;
        }
        return Status.from(value.toString());
    }
}

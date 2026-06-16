package roomescape.reservationtime.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import roomescape.reservationtime.domain.ReservationTime;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface JpaReservationTimeRepository extends JpaRepository<ReservationTime, Long> {

    @Override
    @Query("""
            SELECT reservationTime
            FROM ReservationTime reservationTime
            WHERE reservationTime.deletedAt IS NULL
            ORDER BY reservationTime.id
            """)
    List<ReservationTime> findAll();

    @Override
    @Query("""
            SELECT reservationTime
            FROM ReservationTime reservationTime
            WHERE reservationTime.id = :id
              AND reservationTime.deletedAt IS NULL
            """)
    Optional<ReservationTime> findById(@Param("id") Long id);

    @Query("""
            SELECT COUNT(reservationTime) > 0
            FROM ReservationTime reservationTime
            WHERE reservationTime.startAt = :startAt
              AND reservationTime.deletedAt IS NULL
            """)
    boolean existsByStartAt(@Param("startAt") LocalTime startAt);
}

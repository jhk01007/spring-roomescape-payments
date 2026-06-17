package roomescape.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface JpaReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findAllByStatusAndPaymentExpiresAtLessThanEqual(Status status, LocalDateTime paymentExpiresAt);

    @Query("""
            SELECT COUNT(reservation) > 0
            FROM Reservation reservation
            WHERE reservation.date = :date
              AND reservation.time.id = :timeId
              AND reservation.theme.id = :themeId
              AND reservation.status != :status
            """)
    boolean existsByDateAndTimeIdAndThemeId(
            @Param("date") LocalDate date,
            @Param("timeId") Long timeId,
            @Param("themeId") Long themeId,
            @Param("status") Status status
    );
}

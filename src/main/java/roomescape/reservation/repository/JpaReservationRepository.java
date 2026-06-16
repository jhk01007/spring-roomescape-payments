package roomescape.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.reservation.domain.Reservation;

public interface JpaReservationRepository extends JpaRepository<Reservation, Long> {
}

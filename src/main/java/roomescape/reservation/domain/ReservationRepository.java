package roomescape.reservation.domain;

import roomescape.reservation.repository.dto.ReservationWaitingDto;
import roomescape.common.dto.PageResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    Optional<Reservation> findById(Long id);

    Optional<ReservationWaitingDto> findWaitingById(Long id);

    PageResult<Reservation> findAllByStatusCanceledNot(int page, int size);

    List<Reservation> findAllPendingPaymentExpiresAtBeforeOrEqual(LocalDateTime paymentExpiresAt);

    List<ReservationWaitingDto> findWaitingAllByGuestName(String guestName);

    Optional<Reservation> findBySlotAndStatusWaitingAndWaitingNumberIsOne(LocalDate date, Long timeId, Long themeId);

    Reservation save(Reservation reservation);

    boolean existsBySlotAndGuestNameExceptCanceled(LocalDate date, Long timeId, Long themeId, String guestName);

    boolean existsBySlotAndStatusConfirmedOrPending(LocalDate date, Long timeId, Long themeId);

    boolean existByTimeId(Long timeId);

    boolean existByThemeId(Long themeId);

    boolean existsBySlot(LocalDate date, Long timeId, Long themeId);
}

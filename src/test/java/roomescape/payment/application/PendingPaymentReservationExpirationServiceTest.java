package roomescape.payment.application;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationRepository;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.test_config.clock.MutableClock;
import roomescape.test_config.fixture.SQLFixtureGenerator;
import roomescape.test_config.integration.db.service.ServiceTest;
import roomescape.theme.domain.Theme;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static roomescape.reservation.domain.Status.CANCELED;
import static roomescape.reservation.domain.Status.CONFIRMED;
import static roomescape.reservation.domain.Status.PENDING;

@ServiceTest
class PendingPaymentReservationExpirationServiceTest {

    @Autowired
    PendingPaymentReservationExpirationService expirationService;

    @Autowired
    ReservationRepository reservationRepository;

    @Autowired
    SQLFixtureGenerator fixtureGenerator;

    @Autowired
    MutableClock clock;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("마지막 수정 시각이 10분 이상 지난 결제 대기 예약을 취소한다.")
    void expire_success() {
        // given
        LocalDateTime now = LocalDateTime.of(2025, 5, 1, 12, 0);
        clock.setFixed(now);

        Reservation expiredPending = insertReservation("브라운", LocalTime.of(10, 0), PENDING, now.minusMinutes(10));
        Reservation recentPending = insertReservation("포비", LocalTime.of(12, 0), PENDING, now.minusMinutes(9));
        Reservation oldConfirmed = insertReservation("호눅스", LocalTime.of(14, 0), CONFIRMED, now.minusMinutes(20));

        // when
        int expiredCount = expirationService.expire();

        // then
        entityManager.flush();
        entityManager.clear();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(reservationRepository.findById(expiredPending.getId()).get().getStatus()).isEqualTo(CANCELED);
        assertThat(reservationRepository.findById(recentPending.getId()).get().getStatus()).isEqualTo(PENDING);
        assertThat(reservationRepository.findById(oldConfirmed.getId()).get().getStatus()).isEqualTo(CONFIRMED);
    }

    private Reservation insertReservation(
            String guestName,
            LocalTime startAt,
            roomescape.reservation.domain.Status status,
            LocalDateTime lastModifiedAt
    ) {
        ReservationTime time = fixtureGenerator.insertReservationTime(startAt);
        Theme theme = fixtureGenerator.insertTheme(
                guestName + " 테마",
                "우테코 레벨2를 탈출하는 내용입니다.",
                "https://example.com/theme.png",
                40_000L
        );
        return fixtureGenerator.insertReservation(
                guestName,
                LocalDate.of(2025, 5, 2),
                time,
                theme,
                status,
                lastModifiedAt
        );
    }
}

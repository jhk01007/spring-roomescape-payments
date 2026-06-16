package roomescape.reservationtime.repository;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.reservationtime.domain.ReservationTimeRepository;
import roomescape.test_config.fixture.SQLFixtureGenerator;
import roomescape.test_config.integration.db.repository.RepositoryTest;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
class ReservationTimeRepositoryTest {

    @Autowired
    private ReservationTimeRepository reservationTimeRepository;

    @Autowired
    private SQLFixtureGenerator sqlFixtureGenerator;

    @Test
    @DisplayName("예약 시간을 저장한다.")
    void save() {
        // given
        ReservationTime reservationTime = ReservationTime.create(LocalTime.of(10, 0));

        // when
        ReservationTime saved = reservationTimeRepository.save(reservationTime);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStartAt()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("삭제된 예약 시간은 id로 조회되지 않는다.")
    void findById_softDelete() {
        // given
        ReservationTime reservationTime = sqlFixtureGenerator.insertDeletedReservationTime(LocalTime.of(10, 0));

        // when
        Optional<ReservationTime> found = reservationTimeRepository.findById(reservationTime.getId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("모든 예약 시간 목록을 조회한다")
    void findAll() {
        sqlFixtureGenerator.insertReservationTime(LocalTime.of(10, 0));

        List<ReservationTime> reservationTimes = reservationTimeRepository.findAll();

        assertThat(reservationTimes).hasSize(1);
        assertThat(reservationTimes.getFirst().getStartAt()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("예약 시간 목록은 삭제되지 않은 예약 시간만 조회한다.")
    void findAll_softDelete() {
        // given
        sqlFixtureGenerator.insertDeletedReservationTime(LocalTime.of(10, 0));
        ReservationTime activeTime = sqlFixtureGenerator.insertReservationTime(LocalTime.of(12, 0));

        // when
        List<ReservationTime> reservationTimes = reservationTimeRepository.findAll();

        // then
        assertThat(reservationTimes)
                .extracting(ReservationTime::getId, ReservationTime::getStartAt)
                .containsExactly(Tuple.tuple(activeTime.getId(), activeTime.getStartAt()));
    }

    @Test
    @DisplayName("예약 시간 존재 여부를 조회한다.")
    void existsByStartAt() {
        sqlFixtureGenerator.insertReservationTime(LocalTime.of(10, 0));

        boolean exists = reservationTimeRepository.existsByStartAt(LocalTime.of(10, 0));

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("삭제된 예약 시간은 존재하지 않는 것으로 조회한다.")
    void existsByStartAt_softDelete() {
        // given
        sqlFixtureGenerator.insertDeletedReservationTime(LocalTime.of(10, 0));

        // when
        boolean exists = reservationTimeRepository.existsByStartAt(LocalTime.of(10, 0));

        // then
        assertThat(exists).isFalse();
    }

}

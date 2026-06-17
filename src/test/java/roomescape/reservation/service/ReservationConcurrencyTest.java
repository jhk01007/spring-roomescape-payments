package roomescape.reservation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.dto.PageResult;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.Status;
import roomescape.reservation.repository.ReservationRepositoryImpl;
import roomescape.reservation.domain.ReservationRepository;
import roomescape.reservation.repository.dto.ReservationWaitingDto;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.test_config.clock.MutableClock;
import roomescape.test_config.integration.db.service.ServiceTest;
import roomescape.test_config.fixture.SQLFixtureGenerator;
import roomescape.theme.domain.Theme;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@ServiceTest
@Import(ReservationConcurrencyTest.ConcurrencyTestConfig.class)
@Sql(value = "/acceptance-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReservationConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private SQLFixtureGenerator sqlFixtureGenerator;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private MutableClock clock;

    @Test
    @DisplayName("동시에 같은 날짜, 시간, 테마로 대기 예약하면 모두 대기 예약으로 생성된다.")
    void createWaiting_concurrently_sameOccupiedSlot_allWaiting() throws Exception {
        // given
        clock.setFixed(LocalDate.of(2025, 5, 10));

        ReservationTime time = sqlFixtureGenerator.insertReservationTime(LocalTime.of(10, 0));
        Theme theme = sqlFixtureGenerator.insertTheme("레벨2 탈출", "우테코 레벨2를 탈출하는 내용입니다.", "https://example.com/theme.png");
        LocalDate date = LocalDate.of(2025, 5, 11);
        sqlFixtureGenerator.insertReservation("제이미", date, time, theme, Status.CONFIRMED);

        // when
        executeConcurrently(
                () -> reservationService.createWaiting("브라운", date, time.getId(), theme.getId()),
                () -> reservationService.createWaiting("포비", date, time.getId(), theme.getId())
        );

        // then
        long waitingCount = countWaitingReservations(date, time.getId(), theme.getId());
        assertThat(waitingCount).isEqualTo(2);
    }

    private void executeConcurrently(Runnable first, Runnable second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            Future<?> firstFuture = executor.submit(() -> {
                await(startLatch);
                first.run();
            });
            Future<?> secondFuture = executor.submit(() -> {
                await(startLatch);
                second.run();
            });

            startLatch.countDown();
            firstFuture.get(3, TimeUnit.SECONDS);
            secondFuture.get(3, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private long countWaitingReservations(LocalDate date, Long timeId, Long themeId) {
        Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM reservation
                        WHERE date = :date
                          AND time_id = :timeId
                          AND theme_id = :themeId
                          AND status = 'WAITING'
                        """,
                new MapSqlParameterSource()
                        .addValue("date", Date.valueOf(date))
                        .addValue("timeId", timeId)
                        .addValue("themeId", themeId),
                Long.class);

        return count == null ? 0 : count;
    }

    @TestConfiguration
    static class ConcurrencyTestConfig {

        @Bean
        @Primary
        ReservationRepository synchronizedReservationRepository(ReservationRepositoryImpl delegate) {
            return new SynchronizedReservationRepository(delegate);
        }
    }

    private static class SynchronizedReservationRepository implements ReservationRepository {

        private final ReservationRepository delegate;
        private final CyclicBarrier concurrentCreateBarrier = new CyclicBarrier(2);

        private SynchronizedReservationRepository(ReservationRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<Reservation> findById(Long id) {
            return delegate.findById(id);
        }

        @Override
        public Optional<ReservationWaitingDto> findWaitingById(Long id) {
            return delegate.findWaitingById(id);
        }

        @Override
        public PageResult<Reservation> findAllByStatusCanceledNot(int page, int size) {
            return delegate.findAllByStatusCanceledNot(page, size);
        }

        @Override
        public List<ReservationWaitingDto> findWaitingAllByGuestName(String guestName) {
            return delegate.findWaitingAllByGuestName(guestName);
        }

        @Override
        public Optional<Reservation> findBySlotAndStatusWaitingAndWaitingNumberIsOne(
                LocalDate date, Long timeId, Long themeId) {
            return delegate.findBySlotAndStatusWaitingAndWaitingNumberIsOne(date, timeId, themeId);
        }

        @Override
        public Reservation save(Reservation reservation) {
            return delegate.save(reservation);
        }

        @Override
        public boolean existsBySlotAndGuestNameExceptCanceled(
                LocalDate date, Long timeId, Long themeId, String guestName) {
            return delegate.existsBySlotAndGuestNameExceptCanceled(date, timeId, themeId, guestName);
        }

        @Override
        public boolean existsBySlotAndStatusConfirmedOrPending(LocalDate date, Long timeId, Long themeId) {
            boolean exists = delegate.existsBySlotAndStatusConfirmedOrPending(date, timeId, themeId);
            awaitConcurrentCreate();
            return exists;
        }

        @Override
        public boolean existByTimeId(Long timeId) {
            return delegate.existByTimeId(timeId);
        }

        @Override
        public boolean existByThemeId(Long themeId) {
            return delegate.existByThemeId(themeId);
        }

        @Override
        public boolean existsBySlot(LocalDate date, Long timeId, Long themeId) {
            return delegate.existsBySlot(date, timeId, themeId);
        }

        private void awaitConcurrentCreate() {
            try {
                concurrentCreateBarrier.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } catch (BrokenBarrierException | TimeoutException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}

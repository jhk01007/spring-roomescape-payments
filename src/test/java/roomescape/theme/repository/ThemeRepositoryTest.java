package roomescape.theme.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import roomescape.reservation.domain.Status;
import roomescape.reservationtime.domain.ReservationTime;
import roomescape.test_config.integration.db.repository.RepositoryTest;
import roomescape.test_config.fixture.SQLFixtureGenerator;
import roomescape.theme.domain.Theme;
import roomescape.theme.domain.ThemeRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@RepositoryTest
class ThemeRepositoryTest {

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private SQLFixtureGenerator sqlFixtureGenerator;

    @Test
    @DisplayName("Theme를 저장하고 조회한다.")
    public void saveAndFindById() {
        Theme theme = themeRepository.save(Theme.create("kim", "desc1", "thumb1"));

        Optional<Theme> found = themeRepository.findById(theme.getId());

        assertThat(found).isPresent();
        Theme savedTheme = found.get();
        assertThat(savedTheme.getId()).isEqualTo(theme.getId());
        assertThat(savedTheme.getName()).isEqualTo("kim");
        assertThat(savedTheme.getDescription()).isEqualTo("desc1");
        assertThat(savedTheme.getThumbnail()).isEqualTo("thumb1");
    }

    @Test
    @DisplayName("삭제된 Theme는 id로 조회되지 않는다.")
    public void findById_softDelete() {
        // given
        Theme theme = sqlFixtureGenerator.insertDeletedTheme("kim", "desc1", "thumb1");

        // when
        Optional<Theme> found = themeRepository.findById(theme.getId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("모든 Theme를 불러온다.")
    public void findAll() {
        sqlFixtureGenerator.insertTheme("kim", "desc1", "thumb1");
        sqlFixtureGenerator.insertTheme("lee", "desc2", "thumb2");
        sqlFixtureGenerator.insertTheme("park", "desc3", "thumb3");

        List<Theme> themes = themeRepository.findAll();

        assertThat(themes).hasSize(3)
                .extracting(
                        Theme::getName,
                        Theme::getDescription,
                        Theme::getThumbnail
                ).containsExactlyInAnyOrder(
                        tuple("kim", "desc1", "thumb1"),
                        tuple("lee", "desc2", "thumb2"),
                        tuple("park", "desc3", "thumb3")
                );
    }

    @Test
    @DisplayName("Theme 목록은 삭제되지 않은 Theme만 조회한다.")
    public void findAll_softDelete() {
        // given
        sqlFixtureGenerator.insertDeletedTheme("kim", "desc1", "thumb1");
        Theme activeTheme = sqlFixtureGenerator.insertTheme("lee", "desc2", "thumb2");

        // when
        List<Theme> themes = themeRepository.findAll();

        // then
        assertThat(themes)
                .extracting(Theme::getId, Theme::getName)
                .containsExactly(tuple(activeTheme.getId(), activeTheme.getName()));
    }

    @Test
    @DisplayName("Theme 존재 여부를 조회한다.")
    public void existsById() {
        Theme theme = sqlFixtureGenerator.insertTheme("kim", "desc1", "thumb1");

        boolean exists = themeRepository.existsById(theme.getId());
        boolean notExists = themeRepository.existsById(theme.getId() + 1);

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("삭제된 Theme는 존재하지 않는 것으로 조회한다.")
    public void existsById_softDelete() {
        // given
        Theme theme = sqlFixtureGenerator.insertDeletedTheme("kim", "desc1", "thumb1");

        // when
        boolean exists = themeRepository.existsById(theme.getId());

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("지정된 날짜 기간의 지정된 갯수 만큼의 테마정보를 예약 수를 순서대로 불러온다.")
    @Sql(scripts = "/popular-theme-data.sql")
    public void findTopThemesByReservationCount() {
        // given - @/popular-theme-data.sql

        // when
        List<Theme> topThemes = themeRepository.findTopThemesByReservationCount(
                LocalDate.of(2026, 4, 29),
                LocalDate.of(2026, 5, 5),
                10
        );

        // then
        assertThat(topThemes).hasSizeLessThanOrEqualTo(10)
                .extracting(Theme::getId)
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
    }

    @Test
    @DisplayName("인기 테마 조회는 삭제된 예약을 집계에서 제외한다.")
    public void findTopThemesByReservationCount_softDelete() {
        // given
        Theme activeTheme = sqlFixtureGenerator.insertTheme(
                "레벨2 탈출",
                "우테코 레벨2를 탈출하는 내용입니다.",
                "https://example.com/theme.png"
        );
        Theme deletedTheme = sqlFixtureGenerator.insertTheme(
                "레벨3 탈출",
                "우테코 레벨3을 탈출하는 내용입니다.",
                "https://example.com/theme.png"
        );
        ReservationTime time = sqlFixtureGenerator.insertReservationTime(LocalTime.of(10, 0));
        ReservationTime otherTime = sqlFixtureGenerator.insertReservationTime(LocalTime.of(12, 0));
        LocalDate targetDate = LocalDate.of(2026, 5, 1);

        sqlFixtureGenerator.insertReservation("브라운", targetDate, time, activeTheme, Status.WAITING);
        sqlFixtureGenerator.insertDeletedReservation("포비", targetDate, otherTime, deletedTheme);

        // when
        List<Theme> topThemes = themeRepository.findTopThemesByReservationCount(
                LocalDate.of(2026, 4, 29),
                LocalDate.of(2026, 5, 5),
                10
        );

        // then
        assertThat(topThemes)
                .extracting(Theme::getId)
                .containsExactly(activeTheme.getId())
                .doesNotContain(deletedTheme.getId());
    }

    @Test
    @DisplayName("인기 테마 조회는 삭제된 Theme를 집계에서 제외한다.")
    public void findTopThemesByReservationCount_deletedTheme() {
        // given
        Theme activeTheme = sqlFixtureGenerator.insertTheme(
                "레벨2 탈출",
                "우테코 레벨2를 탈출하는 내용입니다.",
                "https://example.com/theme.png"
        );
        Theme deletedTheme = sqlFixtureGenerator.insertDeletedTheme("레벨3 탈출", "우테코 레벨3을 탈출하는 내용입니다.", "https://example.com/theme.png");
        ReservationTime time = sqlFixtureGenerator.insertReservationTime(LocalTime.of(10, 0));
        ReservationTime otherTime = sqlFixtureGenerator.insertReservationTime(LocalTime.of(12, 0));
        LocalDate targetDate = LocalDate.of(2026, 5, 1);

        sqlFixtureGenerator.insertReservation("브라운", targetDate, time, activeTheme, Status.WAITING);
        sqlFixtureGenerator.insertReservation("포비", targetDate, otherTime, deletedTheme, Status.WAITING);

        // when
        List<Theme> topThemes = themeRepository.findTopThemesByReservationCount(
                LocalDate.of(2026, 4, 29),
                LocalDate.of(2026, 5, 5),
                10
        );

        // then
        assertThat(topThemes)
                .extracting(Theme::getId)
                .containsExactly(activeTheme.getId())
                .doesNotContain(deletedTheme.getId());
    }

}

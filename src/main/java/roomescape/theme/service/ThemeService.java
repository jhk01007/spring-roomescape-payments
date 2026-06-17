package roomescape.theme.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.DomainException;
import roomescape.theme.domain.Theme;
import roomescape.reservation.domain.ReservationRepository;
import roomescape.theme.domain.ThemeRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static roomescape.theme.exception.ThemeErrorCode.*;

@Service
@RequiredArgsConstructor
public class ThemeService {
    private final ThemeRepository themeRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    @Transactional
    public Theme create(String name, String description, String thumbnail) {
        return create(name, description, thumbnail, Theme.DEFAULT_PRICE);
    }

    @Transactional
    public Theme create(String name, String description, String thumbnail, Long price) {
        Theme theme = Theme.create(name, description, thumbnail, price);

        return themeRepository.save(theme);
    }

    @Transactional(readOnly = true)
    public List<Theme> findAllThemes(){
        return themeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Theme> findPopularThemes(int days, int size){
        LocalDate now = LocalDate.now(clock);
        LocalDate startDate = now.minusDays(days);
        LocalDate endDate = now.minusDays(1);

        return themeRepository.findTopThemesByReservationCount(startDate, endDate, size);
    }

    @Transactional
    public void delete(Long id) {
        if (reservationRepository.existByThemeId(id)) {
            throw new DomainException(THEME_HAS_RESERVATION);
        }

        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new DomainException(THEME_NOT_FOUND));
        theme.cancel(LocalDateTime.now(clock));
    }
}

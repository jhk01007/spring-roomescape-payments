package roomescape.theme.controller.dto;

import roomescape.theme.domain.Theme;

public record ThemeResponse(Long id, String name, String description, String thumbnail, Long price) {

    public static ThemeResponse from(Theme theme) {
        return new ThemeResponse(
                theme.getId(),
                theme.getName(),
                theme.getDescription(),
                theme.getThumbnail(),
                theme.getPrice()
        );
    }
}

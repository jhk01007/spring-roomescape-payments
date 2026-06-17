package roomescape.theme.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import roomescape.theme.domain.Theme;

public record ThemeCreateRequest(
        @NotBlank(message = "테마 이름은 비어 있을 수 없습니다.")
        String name,
        @NotBlank(message = "테마 설명은 비어 있을 수 없습니다.")
        String description,
        @NotBlank(message = "테마 썸네일은 비어 있을 수 없습니다.")
        String thumbnail,
        @Positive(message = "테마 가격은 0보다 커야 합니다.")
        Long price
) {
    public ThemeCreateRequest {
        if (price == null) {
            price = Theme.DEFAULT_PRICE;
        }
    }

    public ThemeCreateRequest(String name, String description, String thumbnail) {
        this(name, description, thumbnail, Theme.DEFAULT_PRICE);
    }
}

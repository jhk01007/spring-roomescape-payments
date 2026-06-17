package roomescape.theme.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import roomescape.common.exception.DomainException;

import java.time.LocalDateTime;
import java.util.Objects;

import static roomescape.common.domain.DomainPreconditions.require;
import static roomescape.common.domain.DomainPreconditions.requireNonBlank;
import static roomescape.common.domain.DomainPreconditions.requireNonNull;
import static roomescape.theme.exception.ThemeErrorCode.*;

@Getter
@Entity
@Table(name = "theme")
public class Theme {
    public static final long DEFAULT_PRICE = 50_000L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String thumbnail;

    @Column(nullable = false)
    private Long price;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Theme() {
    }

    private Theme(
            Long id,
            String name,
            String description,
            String thumbnail,
            Long price,
            LocalDateTime deletedAt
    ) {
        validateTheme(name, description, thumbnail, price);
        this.id = id;
        this.name = name;
        this.description = description;
        this.thumbnail = thumbnail;
        this.price = price;
        this.deletedAt = deletedAt;
    }

    public static Theme create(String name, String description, String thumbnail) {
        return create(name, description, thumbnail, DEFAULT_PRICE);
    }

    public static Theme create(String name, String description, String thumbnail, Long price) {
        return new Theme(null, name, description, thumbnail, price, null);
    }

    public static Theme of(long id, String name, String description, String thumbnail) {
        return of(id, name, description, thumbnail, DEFAULT_PRICE, null);
    }

    public static Theme of(long id, String name, String description, String thumbnail, Long price) {
        return of(id, name, description, thumbnail, price, null);
    }

    public static Theme of(
            long id,
            String name,
            String description,
            String thumbnail,
            LocalDateTime deletedAt
    ) {
        return of(id, name, description, thumbnail, DEFAULT_PRICE, deletedAt);
    }

    public static Theme of(
            long id,
            String name,
            String description,
            String thumbnail,
            Long price,
            LocalDateTime deletedAt
    ) {
        return new Theme(id, name, description, thumbnail, price, deletedAt);
    }

    public Theme withId(long id) {
        require(this.id == null, new DomainException(THEME_ALREADY_HAS_ID));
        return of(id, name, description, thumbnail, price, deletedAt);
    }

    public void cancel(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    private void validateTheme(String name, String description, String thumbnail, Long price) {
        requireNonBlank(name, new DomainException(INVALID_THEME_NAME));
        requireNonBlank(description, new DomainException(INVALID_THEME_DESCRIPTION));
        requireNonBlank(thumbnail, new DomainException(INVALID_THEME_THUMBNAIL));
        requireNonNull(price, new DomainException(INVALID_THEME_PRICE));
        require(price > 0, new DomainException(INVALID_THEME_PRICE));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Theme theme)) return false;
        return id != null && Objects.equals(id, theme.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public boolean isSamePrice(Long price) {
        return Objects.equals(this.price, price);
    }
}

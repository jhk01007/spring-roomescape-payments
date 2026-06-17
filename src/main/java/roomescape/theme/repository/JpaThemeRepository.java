package roomescape.theme.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import roomescape.theme.domain.Theme;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaThemeRepository extends JpaRepository<Theme, Long> {

    @Override
    @Query("""
            SELECT theme
            FROM Theme theme
            WHERE theme.deletedAt IS NULL
            ORDER BY theme.id
            """)
    List<Theme> findAll();

    @Override
    @Query("""
            SELECT theme
            FROM Theme theme
            WHERE theme.id = :id
              AND theme.deletedAt IS NULL
            """)
    Optional<Theme> findById(@Param("id") Long id);

    @Query(value = """
            SELECT
                t.id,
                t.name,
                t.description,
                t.thumbnail,
                t.price,
                t.deleted_at
            FROM theme t
            INNER JOIN reservation r
                ON r.theme_id = t.id
                AND r.status != 'CANCELED'
            WHERE r.date BETWEEN :startDate AND :endDate
                AND t.deleted_at IS NULL
            GROUP BY t.id, t.name, t.description, t.thumbnail, t.price, t.deleted_at
            ORDER BY COUNT(r.id) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Theme> findTopThemesByReservationCount(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit
    );

    @Override
    @Query("""
            SELECT COUNT(theme) > 0
            FROM Theme theme
            WHERE theme.id = :id
              AND theme.deletedAt IS NULL
            """)
    boolean existsById(@Param("id") Long id);
}

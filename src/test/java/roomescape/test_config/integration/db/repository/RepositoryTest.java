package roomescape.test_config.integration.db.repository;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import roomescape.test_config.fixture.SQLFixtureGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@Import({
        AutoRepositoryTestBeanConfig.class,
        SQLFixtureGenerator.class
})
public @interface RepositoryTest {
}

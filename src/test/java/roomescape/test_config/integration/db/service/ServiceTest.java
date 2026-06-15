package roomescape.test_config.integration.db.service;

import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import roomescape.test_config.clock.TestClockConfig;
import roomescape.test_config.fixture.SQLFixtureGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@JdbcTest
@Import({
        AutoServiceTestBeanConfig.class,
        TestClockConfig.class,
        SQLFixtureGenerator.class
})
public @interface ServiceTest {
}

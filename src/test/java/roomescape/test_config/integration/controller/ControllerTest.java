package roomescape.test_config.integration.controller;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@Import(AutoMockControllerDependencyConfig.class)
@ExtendWith({
        MockedBeanFieldInjectionExtension.class,
        AutoMockResetExtension.class
})
public @interface ControllerTest {
}

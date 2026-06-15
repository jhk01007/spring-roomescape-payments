package roomescape.test_config.integration.db.repository;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import roomescape.test_config.integration.db.AutoDbTestBeansInjector;

@TestConfiguration(proxyBeanMethods = false)
public class AutoRepositoryTestBeanConfig {

    @Bean
    static AutoDbTestBeansInjector autoRepositoryTestBeans() {
        return AutoDbTestBeansInjector.repositoryTestBeans();
    }
}

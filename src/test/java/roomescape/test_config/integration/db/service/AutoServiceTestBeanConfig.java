package roomescape.test_config.integration.db.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import roomescape.test_config.integration.db.AutoDbTestBeansInjector;

@TestConfiguration(proxyBeanMethods = false)
class AutoServiceTestBeanConfig {

    @Bean
    static AutoDbTestBeansInjector autoServiceTestBeans() {
        return AutoDbTestBeansInjector.serviceTestBeans();
    }
}

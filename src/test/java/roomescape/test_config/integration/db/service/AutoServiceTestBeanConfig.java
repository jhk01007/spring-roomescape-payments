package roomescape.test_config.integration.db.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import roomescape.payment.application.port.out.PaymentGateway;
import roomescape.payment.domain.PaymentResult;
import roomescape.payment.domain.PaymentStatus;
import roomescape.test_config.integration.db.AutoDbTestBeansInjector;

import java.time.LocalDateTime;

@TestConfiguration(proxyBeanMethods = false)
class AutoServiceTestBeanConfig {

    @Bean
    static AutoDbTestBeansInjector autoServiceTestBeans() {
        return AutoDbTestBeansInjector.serviceTestBeans();
    }

    @Bean
    PaymentGateway paymentGateway() {
        return confirmation -> new PaymentResult(
                confirmation.paymentKey(),
                confirmation.orderId(),
                PaymentStatus.DONE,
                confirmation.amount(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}

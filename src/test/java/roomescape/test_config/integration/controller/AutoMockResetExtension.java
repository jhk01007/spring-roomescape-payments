package roomescape.test_config.integration.controller;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class AutoMockResetExtension implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) {
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        if (!(applicationContext instanceof ConfigurableApplicationContext configurableApplicationContext)) {
            return;
        }

        var beanFactory = configurableApplicationContext.getBeanFactory();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            if (isAutoMockBean(beanFactory.getBeanDefinition(beanName))) {
                Mockito.reset(beanFactory.getBean(beanName));
            }
        }
    }

    private boolean isAutoMockBean(org.springframework.beans.factory.config.BeanDefinition beanDefinition) {
        return Boolean.TRUE.equals(beanDefinition.getAttribute(AutoWebMvcTestMockBeansInjector.AUTO_MOCK_ATTRIBUTE));
    }
}

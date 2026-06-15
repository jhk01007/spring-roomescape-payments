package roomescape.test_config.integration.controller;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

class MockedBeanFieldInjectionExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        Object testInstance = context.getRequiredTestInstance();
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);

        ReflectionUtils.doWithFields(
                testInstance.getClass(),
                field -> injectMockedBean(testInstance, applicationContext, field),
                field -> field.isAnnotationPresent(MockedBean.class)
        );
    }

    private void injectMockedBean(Object testInstance, ApplicationContext applicationContext, Field field) {
        if (Modifier.isFinal(field.getModifiers())) {
            throw new IllegalStateException("@MockedBean cannot inject final field: " + field);
        }

        Object bean = applicationContext.getBean(field.getType());
        if (!Mockito.mockingDetails(bean).isMock()) {
            throw new IllegalStateException("@MockedBean target is not a Mockito mock: " + field);
        }

        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, testInstance, bean);
    }
}

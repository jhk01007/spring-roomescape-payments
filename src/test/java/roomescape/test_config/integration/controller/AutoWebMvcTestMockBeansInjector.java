package roomescape.test_config.integration.controller;

import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

class AutoWebMvcTestMockBeansInjector implements BeanDefinitionRegistryPostProcessor {

    static final String AUTO_MOCK_ATTRIBUTE = "roomescape.autoMock";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Set<Class<?>> dependencyTypes = findControllerDependencyTypes(registry);

        for (Class<?> dependencyType : dependencyTypes) {
            if (!containsBeanOfType(registry, dependencyType)) {
                registerMockBean(registry, dependencyType);
            }
        }
    }

    private Set<Class<?>> findControllerDependencyTypes(BeanDefinitionRegistry registry) {
        Set<Class<?>> dependencyTypes = new LinkedHashSet<>();

        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            Class<?> beanClass = resolveBeanClass(beanDefinition).orElse(null);

            if (beanClass == null || !isController(beanClass)) {
                continue;
            }

            findAutowiredConstructor(beanClass)
                    .stream()
                    .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                    .filter(this::isMockableDependency)
                    .forEach(dependencyTypes::add);
        }

        return dependencyTypes;
    }

    private Optional<Class<?>> resolveBeanClass(BeanDefinition beanDefinition) {
        if (beanDefinition instanceof AbstractBeanDefinition abstractBeanDefinition) {
            Class<?> targetType = abstractBeanDefinition.getResolvableType().resolve();
            if (targetType != null) {
                return Optional.of(targetType);
            }
        }

        String beanClassName = beanDefinition.getBeanClassName();
        if (beanClassName == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(ClassUtils.forName(beanClassName, getClass().getClassLoader()));
        } catch (ClassNotFoundException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private boolean isController(Class<?> beanClass) {
        return AnnotatedElementUtils.hasAnnotation(beanClass, Controller.class);
    }

    private Optional<Constructor<?>> findAutowiredConstructor(Class<?> beanClass) {
        return Arrays.stream(beanClass.getDeclaredConstructors())
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .filter(constructor -> constructor.getParameterCount() > 0);
    }

    private boolean isMockableDependency(Class<?> dependencyType) {
        return !dependencyType.isPrimitive()
                && !dependencyType.isArray()
                && !dependencyType.getName().startsWith("java.");
    }

    private boolean containsBeanOfType(BeanDefinitionRegistry registry, Class<?> type) {
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            if (resolveBeanClass(beanDefinition)
                    .filter(type::isAssignableFrom)
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    private void registerMockBean(BeanDefinitionRegistry registry, Class<?> dependencyType) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setTargetType(ResolvableType.forClass(dependencyType));
        beanDefinition.setInstanceSupplier(() -> Mockito.mock(dependencyType));
        beanDefinition.setAttribute(AUTO_MOCK_ATTRIBUTE, true);

        registry.registerBeanDefinition(createBeanName(registry, dependencyType), beanDefinition);
    }

    private String createBeanName(BeanDefinitionRegistry registry, Class<?> dependencyType) {
        String baseName = "autoMock" + dependencyType.getSimpleName();
        String beanName = baseName;
        int index = 2;

        while (registry.containsBeanDefinition(beanName)) {
            beanName = baseName + index++;
        }

        return beanName;
    }
}

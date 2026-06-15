package roomescape.test_config.integration.db;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class AutoDbTestBeansInjector implements BeanDefinitionRegistryPostProcessor {

    private static final String BASE_PACKAGE = "roomescape";

    private final TestSlice testSlice;

    private AutoDbTestBeansInjector(TestSlice testSlice) {
        this.testSlice = testSlice;
    }

    public static AutoDbTestBeansInjector serviceTestBeans() {
        return new AutoDbTestBeansInjector(TestSlice.SERVICE);
    }

    public static AutoDbTestBeansInjector repositoryTestBeans() {
        return new AutoDbTestBeansInjector(TestSlice.REPOSITORY);
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        findCandidateTypes().stream()
                .filter(testSlice::supports)
                .forEach(type -> registerActualBean(registry, type));
    }

    private Set<Class<?>> findCandidateTypes() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        Set<Class<?>> types = new LinkedHashSet<>();
        for (BeanDefinition beanDefinition : scanner.findCandidateComponents(BASE_PACKAGE)) {
            resolveClass(beanDefinition.getBeanClassName()).ifPresent(types::add);
        }
        return types;
    }

    private Optional<Class<?>> resolveClass(String className) {
        if (className == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ClassUtils.forName(className, getClass().getClassLoader()));
        } catch (ClassNotFoundException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private void registerActualBean(BeanDefinitionRegistry registry, Class<?> type) {
        if (containsBeanOfType(registry, type)) {
            return;
        }

        RootBeanDefinition beanDefinition = new RootBeanDefinition(type);
        beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);

        registry.registerBeanDefinition(createBeanName(registry, type), beanDefinition);
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

    private Optional<Class<?>> resolveBeanClass(BeanDefinition beanDefinition) {
        if (beanDefinition instanceof AbstractBeanDefinition abstractBeanDefinition) {
            Class<?> targetType = abstractBeanDefinition.getResolvableType().resolve();
            if (targetType != null) {
                return Optional.of(targetType);
            }
        }

        return resolveClass(beanDefinition.getBeanClassName());
    }

    private String createBeanName(BeanDefinitionRegistry registry, Class<?> type) {
        String baseName = ClassUtils.getShortNameAsProperty(type);
        String beanName = baseName;
        int index = 2;

        while (registry.containsBeanDefinition(beanName)) {
            beanName = baseName + index++;
        }

        return beanName;
    }

    private enum TestSlice {
        SERVICE {
            @Override
            boolean supports(Class<?> type) {
                return isService(type)
                        || isRepository(type)
                        || isServiceSupportComponent(type);
            }
        },
        REPOSITORY {
            @Override
            boolean supports(Class<?> type) {
                return isRepository(type);
            }
        };

        abstract boolean supports(Class<?> type);

        static boolean isService(Class<?> type) {
            return AnnotatedElementUtils.hasAnnotation(type, Service.class);
        }

        static boolean isRepository(Class<?> type) {
            return AnnotatedElementUtils.hasAnnotation(type, Repository.class);
        }

        static boolean isServiceSupportComponent(Class<?> type) {
            return AnnotatedElementUtils.hasAnnotation(type, Component.class)
                    && type.getPackageName().contains(".service.");
        }
    }
}

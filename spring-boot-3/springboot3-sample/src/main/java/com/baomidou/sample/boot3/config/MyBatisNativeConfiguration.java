/*
 * Copyright © ${project.inceptionYear} organization baomidou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.sample.boot3.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * MyBatis 的 Spring AOT 集成。
 *
 * <p>取自 MyBatis 官方的 MyBatisNativeConfiguration 模板；静态的 GraalVM 元数据（日志实现、 缓存装饰器、dtd/xsd 资源等）已按依赖分目录改为
 * {@code META-INF/native-image/<groupId>/<artifactId>/<version>/reachability-metadata.json}，
 * 这里只保留无法用静态元数据表达的部分：
 *
 * <ul>
 *   <li>{@code MyBatisMapperFactoryBeanPostProcessor}：为 MapperFactoryBean 的 BeanDefinition
 *       补齐类型信息，避免 AOT 代码生成退回到自动装配 Class；
 *   <li>{@code MyBatisBeanFactoryInitializationAotProcessor}：处理 mapper 扫描器生成的 MapperFactoryBean
 *       定义，并为 mapper 接口及其实体注册反射（由 AOT 扫描结果驱动）。
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class MyBatisNativeConfiguration {

    @Bean
    static MyBatisMapperFactoryBeanPostProcessor myBatisMapperFactoryBeanPostProcessor() {
        return new MyBatisMapperFactoryBeanPostProcessor();
    }

    @Bean
    MyBatisBeanFactoryInitializationAotProcessor myBatisBeanFactoryInitializationAotProcessor() {
        return new MyBatisBeanFactoryInitializationAotProcessor();
    }

    static class MyBatisBeanFactoryInitializationAotProcessor
            implements BeanFactoryInitializationAotProcessor, BeanRegistrationExcludeFilter {

        private final Set<Class<?>> excludeClasses = new HashSet<>();

        MyBatisBeanFactoryInitializationAotProcessor() {
            excludeClasses.add(MapperScannerConfigurer.class);
        }

        @Override
        public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
            return excludeClasses.contains(registeredBean.getBeanClass());
        }

        @Override
        public BeanFactoryInitializationAotContribution processAheadOfTime(
                ConfigurableListableBeanFactory beanFactory) {
            String[] beanNames = beanFactory.getBeanNamesForType(MapperFactoryBean.class);
            if (beanNames.length == 0) {
                return null;
            }
            return (context, code) -> {
                RuntimeHints hints = context.getRuntimeHints();
                for (String beanName : beanNames) {
                    BeanDefinition beanDefinition =
                            beanFactory.getBeanDefinition(beanName.substring(1));
                    PropertyValue mapperInterface =
                            beanDefinition.getPropertyValues().getPropertyValue("mapperInterface");
                    if (mapperInterface != null && mapperInterface.getValue() != null) {
                        Class<?> mapperInterfaceType = (Class<?>) mapperInterface.getValue();
                        if (mapperInterfaceType != null) {
                            registerReflectionTypeIfNecessary(mapperInterfaceType, hints);
                            hints.proxies().registerJdkProxy(mapperInterfaceType);
                            hints.resources()
                                    .registerPattern(
                                            mapperInterfaceType
                                                    .getName()
                                                    .replace('.', '/')
                                                    .concat(".xml"));
                            registerMapperRelationships(mapperInterfaceType, hints);
                        }
                    }
                }
            };
        }

        private void registerMapperRelationships(Class<?> mapperInterfaceType, RuntimeHints hints) {
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(mapperInterfaceType);
            for (Method method : methods) {
                if (method.getDeclaringClass() != Object.class) {
                    ReflectionUtils.makeAccessible(method);
                    registerSqlProviderTypes(
                            method,
                            hints,
                            SelectProvider.class,
                            SelectProvider::value,
                            SelectProvider::type);
                    registerSqlProviderTypes(
                            method,
                            hints,
                            InsertProvider.class,
                            InsertProvider::value,
                            InsertProvider::type);
                    registerSqlProviderTypes(
                            method,
                            hints,
                            UpdateProvider.class,
                            UpdateProvider::value,
                            UpdateProvider::type);
                    registerSqlProviderTypes(
                            method,
                            hints,
                            DeleteProvider.class,
                            DeleteProvider::value,
                            DeleteProvider::type);
                    Class<?> returnType =
                            MyBatisMapperTypeUtils.resolveReturnClass(mapperInterfaceType, method);
                    registerReflectionTypeIfNecessary(returnType, hints);
                    MyBatisMapperTypeUtils.resolveParameterClasses(mapperInterfaceType, method)
                            .forEach(x -> registerReflectionTypeIfNecessary(x, hints));
                }
            }
        }

        @SafeVarargs
        private <T extends Annotation> void registerSqlProviderTypes(
                Method method,
                RuntimeHints hints,
                Class<T> annotationType,
                Function<T, Class<?>>... providerTypeResolvers) {
            for (T annotation : method.getAnnotationsByType(annotationType)) {
                for (Function<T, Class<?>> providerTypeResolver : providerTypeResolvers) {
                    registerReflectionTypeIfNecessary(
                            providerTypeResolver.apply(annotation), hints);
                }
            }
        }

        private void registerReflectionTypeIfNecessary(Class<?> type, RuntimeHints hints) {
            if (!type.isPrimitive() && !type.getName().startsWith("java")) {
                hints.reflection().registerType(type, MemberCategory.values());
            }
        }
    }

    static class MyBatisMapperTypeUtils {
        /** 工具类，禁止实例化。 */
        private MyBatisMapperTypeUtils() {}

        static Class<?> resolveReturnClass(Class<?> mapperInterface, Method method) {
            Type resolvedReturnType =
                    TypeParameterResolver.resolveReturnType(method, mapperInterface);
            return typeToClass(resolvedReturnType, method.getReturnType());
        }

        static Set<Class<?>> resolveParameterClasses(Class<?> mapperInterface, Method method) {
            return Stream.of(TypeParameterResolver.resolveParamTypes(method, mapperInterface))
                    .map(x -> typeToClass(x, x instanceof Class ? (Class<?>) x : Object.class))
                    .collect(Collectors.toSet());
        }

        private static Class<?> typeToClass(Type src, Class<?> fallback) {
            Class<?> result = null;
            if (src instanceof Class<?>) {
                if (((Class<?>) src).isArray()) {
                    result = ((Class<?>) src).getComponentType();
                } else {
                    result = (Class<?>) src;
                }
            } else if (src instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) src;
                int index =
                        (parameterizedType.getRawType() instanceof Class
                                        && Map.class.isAssignableFrom(
                                                (Class<?>) parameterizedType.getRawType())
                                        && parameterizedType.getActualTypeArguments().length > 1)
                                ? 1
                                : 0;
                Type actualType = parameterizedType.getActualTypeArguments()[index];
                result = typeToClass(actualType, fallback);
            }
            if (result == null) {
                result = fallback;
            }
            return result;
        }
    }

    static class MyBatisMapperFactoryBeanPostProcessor
            implements MergedBeanDefinitionPostProcessor, BeanFactoryAware {

        private static final org.apache.commons.logging.Log LOG =
                LogFactory.getLog(MyBatisMapperFactoryBeanPostProcessor.class);

        private static final String MAPPER_FACTORY_BEAN =
                "org.mybatis.spring.mapper.MapperFactoryBean";

        private ConfigurableBeanFactory beanFactory;

        @Override
        public void setBeanFactory(BeanFactory beanFactory) {
            this.beanFactory = (ConfigurableBeanFactory) beanFactory;
        }

        @Override
        public void postProcessMergedBeanDefinition(
                RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
            if (ClassUtils.isPresent(MAPPER_FACTORY_BEAN, this.beanFactory.getBeanClassLoader())) {
                resolveMapperFactoryBeanTypeIfNecessary(beanDefinition);
            }
        }

        /**
         * 为 {@code MapperFactoryBean} 的 BeanDefinition 补齐类型信息。
         *
         * <p>向上下文暴露泛型类型信息以避免过早初始化；同时把构造参数重新注册为 {@code Class} 字面量——扫描器只会记录 mapper 类名字符串，这会使 AOT
         * 代码生成退回到 自动装配 {@code MapperFactoryBean} 构造器，native 测试下会尝试自动装配 {@code Class<?>}。
         */
        private void resolveMapperFactoryBeanTypeIfNecessary(RootBeanDefinition beanDefinition) {
            if (!beanDefinition.hasBeanClass()
                    || !MapperFactoryBean.class.isAssignableFrom(beanDefinition.getBeanClass())) {
                return;
            }
            if (beanDefinition.getResolvableType().hasUnresolvableGenerics()) {
                Class<?> mapperInterface = getMapperInterface(beanDefinition);
                if (mapperInterface != null) {
                    beanDefinition.setTargetType(
                            ResolvableType.forClassWithGenerics(
                                    beanDefinition.getBeanClass(), mapperInterface));
                    ConstructorArgumentValues constructorArgumentValues =
                            new ConstructorArgumentValues();
                    constructorArgumentValues.addGenericArgumentValue(mapperInterface);
                    beanDefinition.setConstructorArgumentValues(constructorArgumentValues);
                }
            }
        }

        private Class<?> getMapperInterface(RootBeanDefinition beanDefinition) {
            try {
                return (Class<?>) beanDefinition.getPropertyValues().get("mapperInterface");
            } catch (Exception e) {
                LOG.debug("Fail getting mapper interface type.", e);
                return null;
            }
        }
    }
}

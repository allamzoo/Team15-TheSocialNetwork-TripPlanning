package com.team15.tripplanning.destinationservice.config;

import com.team15.tripplanning.destinationservice.model.DestinationSearchDocument;
import com.team15.tripplanning.destinationservice.repository.DestinationSearchRepository;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchRepositoryConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.data.elasticsearch.repositories.enabled", havingValue = "false", matchIfMissing = true)
    public DestinationSearchRepository destinationSearchRepository() {
        ClassLoader classLoader = DestinationSearchRepository.class.getClassLoader();
        Class<?>[] interfaces = new Class<?>[]{DestinationSearchRepository.class};
        InvocationHandler handler = new NoOpElasticsearchRepositoryHandler();
        return (DestinationSearchRepository) Proxy.newProxyInstance(classLoader, interfaces, handler);
    }

    private static final class NoOpElasticsearchRepositoryHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();

            if (method.getDeclaringClass() == Object.class) {
                return switch (methodName) {
                    case "toString" -> "NoOpDestinationSearchRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (args != null && args.length > 0 ? args[0] : null);
                    default -> null;
                };
            }

            if (methodName.startsWith("save") && args != null && args.length > 0) {
                return args[0];
            }

            if (methodName.startsWith("delete") || methodName.startsWith("remove") || methodName.startsWith("index")) {
                return null;
            }

            Class<?> returnType = method.getReturnType();
            if (returnType == Void.TYPE) {
                return null;
            }
            if (returnType == Boolean.TYPE || returnType == Boolean.class) {
                return false;
            }
            if (returnType == Integer.TYPE || returnType == Integer.class) {
                return 0;
            }
            if (returnType == Long.TYPE || returnType == Long.class) {
                return 0L;
            }
            if (returnType == Double.TYPE || returnType == Double.class) {
                return 0.0d;
            }
            if (returnType == Float.TYPE || returnType == Float.class) {
                return 0.0f;
            }
            if (returnType == Short.TYPE || returnType == Short.class) {
                return (short) 0;
            }
            if (returnType == Byte.TYPE || returnType == Byte.class) {
                return (byte) 0;
            }
            if (returnType == Character.TYPE || returnType == Character.class) {
                return '\0';
            }
            if (Optional.class.isAssignableFrom(returnType)) {
                return Optional.empty();
            }
            if (List.class.isAssignableFrom(returnType)) {
                return Collections.emptyList();
            }
            if (Set.class.isAssignableFrom(returnType)) {
                return Collections.emptySet();
            }
            if (Map.class.isAssignableFrom(returnType)) {
                return Collections.emptyMap();
            }
            if (Collection.class.isAssignableFrom(returnType)) {
                return Collections.emptyList();
            }
            if (returnType.isArray()) {
                return Array.newInstance(returnType.getComponentType(), 0);
            }
            if (returnType == DestinationSearchDocument.class) {
                return null;
            }

            return null;
        }
    }
}


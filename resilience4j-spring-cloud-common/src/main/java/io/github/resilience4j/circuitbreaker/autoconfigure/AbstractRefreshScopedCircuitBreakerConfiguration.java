package io.github.resilience4j.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class AbstractRefreshScopedCircuitBreakerConfiguration {

    protected final ConfigurableBeanFactory beanFactory;
    protected final CircuitBreakerConfiguration circuitBreakerConfiguration;
    protected final CircuitBreakerConfigurationProperties circuitBreakerProperties;

    protected AbstractRefreshScopedCircuitBreakerConfiguration(ConfigurableBeanFactory beanFactory,
                                                        CircuitBreakerConfigurationProperties circuitBreakerProperties) {
        this.beanFactory = beanFactory;
        this.circuitBreakerProperties = circuitBreakerProperties;
        this.circuitBreakerConfiguration = new CircuitBreakerConfiguration(circuitBreakerProperties);
    }

    protected abstract void createHealthIndicatorForCircuitBreaker(CircuitBreaker circuitBreaker, CircuitBreakerConfigurationProperties circuitBreakerProperties);

    /**
     * @param eventConsumerRegistry the circuit breaker event consumer registry
     * @return the RefreshScoped CircuitBreakerRegistry
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry(EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
        CircuitBreakerRegistry circuitBreakerRegistry = circuitBreakerConfiguration.createCircuitBreakerRegistry(circuitBreakerProperties);

        // Register the event consumers
        circuitBreakerConfiguration.registerEventConsumer(circuitBreakerRegistry, eventConsumerRegistry);

        // Register a consumer to hook up any health indicators for circuit breakers after creation. This will catch ones that get
        // created beyond initially configured backends.
        circuitBreakerRegistry.getEventPublisher().onEntryAdded(event -> createHealthIndicatorForCircuitBreaker(event.getAddedEntry(), circuitBreakerProperties));

        // Initialize backends that were initially configured.
        circuitBreakerConfiguration.initCircuitBreakerRegistry(circuitBreakerRegistry);

        return circuitBreakerRegistry;
    }

}

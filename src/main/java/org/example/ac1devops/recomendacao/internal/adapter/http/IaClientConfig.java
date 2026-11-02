package org.example.ac1devops.recomendacao.internal.adapter.http;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IaServiceProperties.class)
public class IaClientConfig {

    @Bean
    CircuitBreaker iaServiceCircuitBreaker(IaServiceProperties properties) {
        return circuitBreaker(properties.circuitOpenDuration());
    }

    /**
     * Abre com 50% de falhas nas ultimas 10 chamadas, depois de pelo menos 5. Aberto, responde sem
     * esperar o timeout; passado {@code openDuration}, 2 chamadas de teste decidem se ele fecha.
     */
    static CircuitBreaker circuitBreaker(Duration openDuration) {
        return CircuitBreaker.of("iaService", CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(openDuration)
                .permittedNumberOfCallsInHalfOpenState(2)
                .build());
    }
}

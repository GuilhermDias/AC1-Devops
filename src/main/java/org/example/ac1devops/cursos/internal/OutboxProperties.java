package org.example.ac1devops.cursos.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Entrega dos eventos: ate {@code maxAttempts} tentativas, com espera que dobra a partir de
 * {@code baseBackoff}. {@code lease} precisa ser maior que o tempo maximo de uma entrega.
 */
@ConfigurationProperties("cursos.outbox")
public record OutboxProperties(
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("5s") Duration baseBackoff,
        @DefaultValue("60s") Duration lease) {
}

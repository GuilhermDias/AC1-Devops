package org.example.ac1devops.recomendacao.internal.adapter.http;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuracao do cliente do servico de IA. O read timeout limita quanto o aluno espera antes do
 * modo degradado; a variavel de ambiente {@code IA_SERVICE_URL} define o endereco.
 */
@ConfigurationProperties("ia.service")
public record IaServiceProperties(
        @DefaultValue("http://localhost:8000") String url,
        @DefaultValue("300ms") Duration connectTimeout,
        @DefaultValue("800ms") Duration readTimeout,
        @DefaultValue("30s") Duration circuitOpenDuration) {
}

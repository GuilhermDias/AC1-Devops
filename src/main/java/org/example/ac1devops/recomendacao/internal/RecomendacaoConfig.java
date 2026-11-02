package org.example.ac1devops.recomendacao.internal;

import org.example.ac1devops.recomendacao.internal.domain.PopularCourses;
import org.example.ac1devops.recomendacao.internal.domain.RecommendationPort;
import org.example.ac1devops.recomendacao.internal.domain.RecommendationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Monta o dominio, que nao tem anotacoes do Spring, com o adapter da porta presente no contexto. */
@Configuration
public class RecomendacaoConfig {

    @Bean
    RecommendationService recommendationService(RecommendationPort recommendationPort) {
        return new RecommendationService(recommendationPort, PopularCourses.defaults());
    }
}

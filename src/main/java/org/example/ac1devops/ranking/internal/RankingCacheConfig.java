package org.example.ac1devops.ranking.internal;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Liga o Spring Cache para o top N do ranking. Sem provedor configurado, o Spring Boot usa um
 * cache em memoria por instancia (ConcurrentMap); com varias replicas, trocar por Redis nao muda
 * o codigo do modulo.
 */
@Configuration
@EnableCaching
public class RankingCacheConfig {

    public static final String TOP_CACHE = "rankingTop";
}

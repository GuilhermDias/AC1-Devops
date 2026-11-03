package org.example.ac1devops.ranking.internal;

import org.example.ac1devops.progression.api.StudentScoreChangedEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Esvazia o cache do top N quando a pontuacao de qualquer aluno muda: um unico aluno pode entrar,
 * sair ou trocar de posicao em todos os tamanhos de top em cache.
 */
@Component
public class RankingCacheInvalidator {

    @EventListener
    @CacheEvict(cacheNames = RankingCacheConfig.TOP_CACHE, allEntries = true)
    public void onScoreChanged(StudentScoreChangedEvent event) {
        // A remocao das entradas e feita pelo @CacheEvict.
    }
}

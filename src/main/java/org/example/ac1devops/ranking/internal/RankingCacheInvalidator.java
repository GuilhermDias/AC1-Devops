package org.example.ac1devops.ranking.internal;

import org.example.ac1devops.progression.api.StudentScoreChangedEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Esvazia o cache do top N quando a pontuacao de qualquer aluno muda: um unico aluno pode entrar,
 * sair ou trocar de posicao em todos os tamanhos de top em cache.
 */
@Component
public class RankingCacheInvalidator {

    @TransactionalEventListener(fallbackExecution = true)
    @CacheEvict(cacheNames = RankingCacheConfig.TOP_CACHE, allEntries = true)
    public void onScoreChanged(StudentScoreChangedEvent event) {
        // A remocao das entradas e feita pelo @CacheEvict.
    }
}

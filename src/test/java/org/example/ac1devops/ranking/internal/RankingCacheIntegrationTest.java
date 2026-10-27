package org.example.ac1devops.ranking.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.example.ac1devops.progression.api.CreateStudentRequestDTO;
import org.example.ac1devops.progression.api.ReceiveXpRequestDTO;
import org.example.ac1devops.progression.api.StudentProgressionApi;
import org.example.ac1devops.ranking.api.RankingApi;
import org.example.ac1devops.ranking.api.RankingEntryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Contexto Spring completo (H2): prova que o top N vem do cache enquanto ninguem pontua e que o
 * {@code StudentScoreChangedEvent} publicado pelo modulo progression invalida o cache.
 * As pontuacoes usadas ficam acima de qualquer valor criado por outros testes do mesmo contexto.
 *
 * <p>As chamadas ao modulo progression rodam em {@link TransactionTemplate}: fora de uma requisicao
 * HTTP nao ha open-in-view, e o {@code StudentService} le a colecao lazy de level-ups ao montar
 * o DTO (dependencia do legado registrada em {@code application.properties}).
 */
@SpringBootTest
class RankingCacheIntegrationTest {

    @Autowired
    private RankingApi ranking;

    @Autowired
    private StudentProgressionApi studentProgression;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache(RankingCacheConfig.TOP_CACHE).clear();
    }

    @Test
    void deveServirTopDoCacheEInvalidarQuandoAPontuacaoMuda() {
        long lider = createWithXp("Lider Cache", 5_000_000);

        List<RankingEntryDTO> first = ranking.getTop(5);
        List<RankingEntryDTO> cached = ranking.getTop(5);

        assertThat(cached).isSameAs(first);
        assertThat(first.get(0).studentId()).isEqualTo(lider);

        long novoLider = createWithXp("Novo Lider Cache", 6_000_000);
        List<RankingEntryDTO> afterChange = ranking.getTop(5);

        assertThat(afterChange).isNotSameAs(first);
        assertThat(afterChange.get(0).studentId()).isEqualTo(novoLider);
        assertThat(afterChange.get(1).studentId()).isEqualTo(lider);
        assertThat(ranking.getPosition(lider).position()).isEqualTo(2);
    }

    @Test
    void naoDeveInvalidarCacheQuandoXpInvalidoNaoMudaAPontuacao() {
        long aluno = createWithXp("Aluno Sem Mudanca", 4_000_000);
        List<RankingEntryDTO> first = ranking.getTop(3);

        transactionTemplate.executeWithoutResult(status ->
                studentProgression.receiveXp(aluno, new ReceiveXpRequestDTO("MOTIVO_INVALIDO", 0)));

        assertThat(ranking.getTop(3)).isSameAs(first);
    }

    private long createWithXp(String name, int xp) {
        return transactionTemplate.execute(status -> {
            long id = studentProgression.createStudent(new CreateStudentRequestDTO(name)).id();
            studentProgression.receiveXp(id, new ReceiveXpRequestDTO("CARGA_TESTE", xp));
            return id;
        });
    }
}

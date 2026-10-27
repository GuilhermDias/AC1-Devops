package org.example.ac1devops.ranking.api;

import java.util.List;

/**
 * Fachada publica do modulo de ranking (RF01). Ordena alunos por XP total (maior primeiro; em
 * empate, quem foi cadastrado antes fica na frente). Usa apenas a API publica do modulo
 * progression.
 */
public interface RankingApi {

    int MAX_PAGE_SIZE = 100;

    /**
     * Pagina do ranking completo, ordenada e paginada no banco.
     *
     * @throws InvalidRankingQueryException se {@code page < 0} ou {@code size} fora de 1..100
     */
    RankingPageDTO getRanking(int page, int size);

    /**
     * Os {@code n} primeiros colocados. Resultado em cache, invalidado sempre que a pontuacao de
     * algum aluno muda.
     *
     * @throws InvalidRankingQueryException se {@code n} fora de 1..100
     */
    List<RankingEntryDTO> getTop(int n);

    /**
     * Posicao de um aluno no ranking.
     *
     * @throws org.example.ac1devops.progression.api.StudentNotFoundException se o aluno nao existir
     */
    RankingEntryDTO getPosition(Long studentId);
}

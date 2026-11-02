package org.example.ac1devops.recomendacao.api;

/**
 * Fachada publica do modulo de recomendacao (RF03 do Desafio 3). Sempre responde: se o servico de
 * IA estiver indisponivel, devolve a lista degradada.
 */
public interface RecomendacaoApi {

    /**
     * @throws org.example.ac1devops.progression.api.StudentNotFoundException se o aluno nao existir
     */
    RecommendationsDTO getRecommendations(Long studentId);
}

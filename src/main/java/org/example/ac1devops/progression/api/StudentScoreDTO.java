package org.example.ac1devops.progression.api;

/**
 * Pontuacao de um aluno (sem o historico de level-ups): projecao leve usada por consultas
 * ordenadas por XP, como a do modulo ranking.
 */
public record StudentScoreDTO(Long id, String name, int xpTotal, Level level) {
}

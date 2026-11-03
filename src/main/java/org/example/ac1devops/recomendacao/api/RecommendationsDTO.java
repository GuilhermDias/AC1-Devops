package org.example.ac1devops.recomendacao.api;

import java.util.List;

/**
 * Recomendacoes de um aluno. {@code degraded} indica que o servico de IA nao respondeu e que a
 * lista veio dos cursos mais populares.
 */
public record RecommendationsDTO(Long studentId, List<CourseDTO> courses, boolean degraded) {
}

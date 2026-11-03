package org.example.ac1devops.cursos.api;

/**
 * Publicado depois do commit da conclusao, com entrega ao menos uma vez: {@code eventId} permite ao
 * consumidor ignorar repeticoes. Se o consumidor lancar excecao, a entrega e repetida mais tarde.
 */
public record CursoConcluidoEvent(Long eventId, Long studentId, String courseId, int xpAwarded) {
}

package org.example.ac1devops.progression.api;

/**
 * Publicado (Spring {@code ApplicationEvent}, em processo) sempre que um aluno e criado ou tem o
 * XP total alterado. Outros modulos reagem a ele sem que o modulo progression os conheca.
 */
public record StudentScoreChangedEvent(Long studentId, int xpTotal) {
}

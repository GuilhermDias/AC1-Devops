package org.example.ac1devops.progression.api;

/**
 * Publicado (Spring {@code ApplicationEvent}, em processo) sempre que um aluno e criado ou tem o
 * XP total alterado. Outros modulos reagem a ele sem que o modulo progression os conheca. Pode ser
 * publicado dentro de uma transacao: os consumidores devem usar {@code @TransactionalEventListener}
 * com {@code fallbackExecution}, para so reagir ao que foi de fato gravado e nunca desfazer a gravacao.
 */
public record StudentScoreChangedEvent(Long studentId, int xpTotal) {
}

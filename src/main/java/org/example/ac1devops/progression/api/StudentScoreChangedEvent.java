package org.example.ac1devops.progression.api;

/**
 * Publicado (Spring {@code ApplicationEvent}, em processo) sempre que um aluno e criado ou tem o
 * XP total alterado. Outros modulos reagem a ele sem que o modulo progression os conheca. Deve ser
 * publicado fora de transacao: os consumidores gravam em transacao propria, e pedir uma segunda conexao
 * enquanto a primeira segue presa pode esgotar o pool sob carga.
 */
public record StudentScoreChangedEvent(Long studentId, int xpTotal) {
}

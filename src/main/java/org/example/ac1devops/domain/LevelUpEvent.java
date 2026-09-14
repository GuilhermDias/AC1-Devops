package org.example.ac1devops.domain;

/**
 * Evento registrado no historico de evolucao do aluno a cada transicao de nivel.
 *
 * <p>Cada faixa de XP atravessada gera um evento proprio: um aluno que salta de 0 para
 * 750 XP de uma unica vez produz tres eventos (Bronze-Prata, Prata-Ouro, Ouro-Diamante).</p>
 */
public class LevelUpEvent {

    private final Level fromLevel;
    private final Level toLevel;

    public LevelUpEvent(Level fromLevel, Level toLevel) {
        this.fromLevel = fromLevel;
        this.toLevel = toLevel;
    }

    public Level getFromLevel() {
        return fromLevel;
    }

    public Level getToLevel() {
        return toLevel;
    }
}

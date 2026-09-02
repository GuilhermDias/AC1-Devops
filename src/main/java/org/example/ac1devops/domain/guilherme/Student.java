package org.example.ac1devops.domain.guilherme;

import java.util.ArrayList;
import java.util.List;

/**
 * Aluno da plataforma de Educacao Continuada Gamificada.
 *
 * <p>POJO puro (sem anotacao de framework) — nesta fase RED/GREEN nao ha JPA, Spring,
 * Service, Repository nem DTO. Sustenta apenas o cenario da US3: continuar acumulando XP
 * no nivel maximo sem gerar novos eventos de level-up.</p>
 */
public class Student {

    /** Registro de uma transicao de nivel no historico de evolucao do aluno. */
    public record LevelChange(Level from, Level to) {
    }

    private final String name;
    private int xpTotal = 0;
    private Level level = Level.BRONZE;
    private final List<LevelChange> levelUpHistory = new ArrayList<>();

    public Student(String name) {
        this.name = name;
    }

    /**
     * Soma {@code amount} ao XP total e recalcula o nivel.
     *
     * <p>Se o novo nivel for diferente do atual, atualiza o nivel e registra a transicao
     * no historico. Se for igual (caso da US3), o historico permanece intacto.</p>
     *
     * @param reason motivo da concessao de XP (ainda nao usado nesta fase)
     * @param amount quantidade de XP recebida
     */
    public void receiveXp(String reason, int amount) {
        xpTotal += amount;
        Level novoNivel = Level.fromXp(xpTotal);
        if (novoNivel != level) {
            levelUpHistory.add(new LevelChange(level, novoNivel));
            level = novoNivel;
        }
    }

    public String getName() {
        return name;
    }

    public int getXpTotal() {
        return xpTotal;
    }

    public Level getLevel() {
        return level;
    }

    public List<LevelChange> getLevelUpHistory() {
        return levelUpHistory;
    }
}

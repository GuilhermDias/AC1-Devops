package org.example.ac1devops.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Aluno da plataforma de Educacao Continuada Gamificada.
 *
 * <p>POJO puro (sem anotacao de framework) — nesta fase RED/GREEN nao ha JPA, Spring,
 * Service, Repository nem DTO. Sustenta os cenarios das US1 a US4: acumular XP, subir de
 * nivel registrando o historico e continuar acumulando no nivel maximo sem gerar novos
 * eventos de level-up.</p>
 */
public class Student {

    private final String name;
    private int xpTotal = 0;
    private Level level = Level.BRONZE;
    private final List<LevelUpEvent> levelUpEvents = new ArrayList<>();

    public Student(String name) {
        this.name = name;
    }

    /**
     * Soma {@code amount} ao XP total e promove o aluno pelas faixas que ele atravessar.
     *
     * <p>Cada faixa ultrapassada gera um {@link LevelUpEvent} proprio, de modo que o
     * historico registra a evolucao completa mesmo quando o aluno pula varios niveis de
     * uma so vez. Se o nivel calculado for igual ao atual (caso da US3, aluno ja em
     * Diamante), nenhum evento e adicionado e o historico permanece intacto.</p>
     *
     * @param reason motivo da concessao de XP (ainda nao usado nesta fase)
     * @param amount quantidade de XP recebida
     */
    public void receiveXp(String reason, int amount) {
        xpTotal += amount;
        Level nivelAlcancado = Level.fromXp(xpTotal);
        while (level.ordinal() < nivelAlcancado.ordinal()) {
            Level proximoNivel = Level.values()[level.ordinal() + 1];
            levelUpEvents.add(new LevelUpEvent(level, proximoNivel));
            level = proximoNivel;
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

    public List<LevelUpEvent> getLevelUpEvents() {
        return levelUpEvents;
    }
}

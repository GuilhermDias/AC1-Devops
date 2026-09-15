package org.example.ac1devops.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Aluno da plataforma de Educacao Continuada Gamificada.
 *
 * <p>POJO puro refatorado para a fase BLUE: mantém a lógica de acumular XP,
 * subir de nível registrando o histórico e continuar acumulando no nível máximo.</p>
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
     * Soma {@code amount} ao XP total e gerencia a promoção de níveis.
     *
     * @param reason motivo da concessao de XP
     * @param amount quantidade de XP recebida
     */
    public void receiveXp(String reason, int amount) {
        if (amount <= 0) {
            return;
        }
        xpTotal += amount;
        processLevelUp();
    }

    /**
     * Extrai a lógica de progressão de níveis para melhorar a legibilidade.
     */
    private void processLevelUp() {
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
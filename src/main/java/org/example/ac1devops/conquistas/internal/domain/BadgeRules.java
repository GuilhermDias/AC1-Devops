package org.example.ac1devops.conquistas.internal.domain;

import java.util.List;
import org.example.ac1devops.progression.api.Level;

/**
 * Catalogo de regras vigentes. E aqui que uma regra muda: criar, alterar ou remover um badge nao
 * exige mudar a porta, os adapters nem o schema.
 */
public final class BadgeRules {

    private BadgeRules() {
    }

    public static List<BadgeRule> defaults() {
        return List.of(
                xpAtLeast(1, "PRIMEIROS_PASSOS", "Recebeu o primeiro XP"),
                levelAtLeast(Level.PRATA, "NIVEL_PRATA", "Alcancou o nivel Prata"),
                levelAtLeast(Level.OURO, "NIVEL_OURO", "Alcancou o nivel Ouro"),
                levelAtLeast(Level.DIAMANTE, "NIVEL_DIAMANTE", "Alcancou o nivel Diamante"),
                xpAtLeast(1000, "MIL_XP", "Acumulou 1000 XP"));
    }

    private static BadgeRule xpAtLeast(int xp, String code, String description) {
        return new BadgeRule(new Badge(code, description), progress -> progress.xpTotal() >= xp);
    }

    private static BadgeRule levelAtLeast(Level level, String code, String description) {
        return new BadgeRule(new Badge(code, description), progress -> progress.level().compareTo(level) >= 0);
    }
}

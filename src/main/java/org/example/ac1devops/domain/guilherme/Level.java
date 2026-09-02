package org.example.ac1devops.domain.guilherme;

/**
 * Niveis de gamificacao e as faixas de XP total necessarias para cada um.
 *
 * <pre>
 * BRONZE    0 - 99
 * PRATA     100 - 299
 * OURO      300 - 699
 * DIAMANTE  700+
 * </pre>
 */
public enum Level {

    BRONZE(0),
    PRATA(100),
    OURO(300),
    DIAMANTE(700);

    private final int minXp;

    Level(int minXp) {
        this.minXp = minXp;
    }

    public int getMinXp() {
        return minXp;
    }

    /**
     * Calcula o nivel correspondente a um XP total, aplicando a tabela de faixas.
     */
    public static Level fromXp(int xpTotal) {
        Level resultado = BRONZE;
        for (Level nivel : values()) {
            if (xpTotal >= nivel.minXp) {
                resultado = nivel;
            }
        }
        return resultado;
    }
}

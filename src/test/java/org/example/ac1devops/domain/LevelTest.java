package org.example.ac1devops.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LevelTest {

    @Test
    void deveExporOXpMinimoDeCadaFaixa() {
        assertThat(Level.BRONZE.getMinXp()).isEqualTo(0);
        assertThat(Level.PRATA.getMinXp()).isEqualTo(100);
        assertThat(Level.OURO.getMinXp()).isEqualTo(300);
        assertThat(Level.DIAMANTE.getMinXp()).isEqualTo(700);
    }
}

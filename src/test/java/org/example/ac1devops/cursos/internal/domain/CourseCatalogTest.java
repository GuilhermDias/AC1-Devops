package org.example.ac1devops.cursos.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CourseCatalogTest {

    @Test
    void deveEncontrarCursoComSuaRecompensa() {
        assertThat(CourseCatalog.find("docker-essencial")).hasValueSatisfying(
                course -> assertThat(course.xpReward()).isEqualTo(100));
    }

    @Test
    void naoDeveEncontrarCursoInexistente() {
        assertThat(CourseCatalog.find("nao-existe")).isEmpty();
    }
}

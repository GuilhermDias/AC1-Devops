package org.example.ac1devops.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudentTest {

    @Test
    void deveSubirDeNivelQuandoXpCruzaFaixaBronzeParaPrata() {
        Student student = new Student("Aluno US1");
        student.receiveXp("ATIVIDADE_CONCLUIDA", 80); // aluno chega a 80 XP, ainda Bronze

        student.receiveXp("ATIVIDADE_CONCLUIDA", 30);

        assertThat(student.getXpTotal()).isEqualTo(110);
        assertThat(student.getLevel()).isEqualTo(Level.PRATA);
        assertThat(student.getLevelUpEvents()).hasSize(1);
        assertThat(student.getLevelUpEvents().get(0).getToLevel()).isEqualTo(Level.PRATA);
    }

    @Test
    void deveAcumularXpSemMudarNivelQuandoPermaneceNaMesmaFaixa() {
        Student student = new Student("Aluno US2");
        student.receiveXp("ATIVIDADE_CONCLUIDA", 10); // aluno chega a 10 XP, Bronze

        student.receiveXp("ATIVIDADE_CONCLUIDA", 20);

        assertThat(student.getXpTotal()).isEqualTo(30);
        assertThat(student.getLevel()).isEqualTo(Level.BRONZE);
        assertThat(student.getLevelUpEvents()).isEmpty();
    }

    @Test
    void naoDeveGerarNovoLevelUpQuandoAlunoJaEstaNoNivelMaximo() {
        Student student = new Student("Aluno US3");
        student.receiveXp("ATIVIDADE_CONCLUIDA", 750); // sobe ate Diamante

        student.receiveXp("ATIVIDADE_CONCLUIDA", 100);

        assertThat(student.getXpTotal()).isEqualTo(850);
        assertThat(student.getLevel()).isEqualTo(Level.DIAMANTE);
        assertThat(student.getLevelUpEvents()).hasSize(3);
    }

    @Test
    void deveRetornarXpTotalNivelEHistoricoDeLevelUpsAoConsultarPerfil() {
        Student student = new Student("Aluno US4");

        student.receiveXp("AULA_CONCLUIDA", 150);
        student.receiveXp("QUIZ_NOTA_ALTA", 200);

        assertThat(student.getXpTotal()).isEqualTo(350);
        assertThat(student.getLevel()).isEqualTo(Level.OURO);
        assertThat(student.getLevelUpEvents()).hasSize(2);
    }
}

package org.example.ac1devops.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudentTest {

    @Test
    @DisplayName("Deve inicializar aluno corretamente com nome, XP zero e nível Bronze")
    void deveInicializarAlunoCorretamente() {
        Student student = new Student("Aluno Teste");

        assertThat(student.getName()).isEqualTo("Aluno Teste");
        assertThat(student.getXpTotal()).isEqualTo(0);
        assertThat(student.getLevel()).isEqualTo(Level.BRONZE);
        assertThat(student.getLevelUpEvents()).isEmpty();
    }

    @Test
    @DisplayName("Deve subir de nível quando XP cruzar faixa de Bronze para Prata")
    void deveSubirDeNivelQuandoXpCruzaFaixaBronzeParaPrata() {
        Student student = new Student("Aluno US1");
        student.receiveXp("ATIVIDADE_CONCLUIDA", 80);
        student.receiveXp("ATIVIDADE_CONCLUIDA", 30);

        assertThat(student.getXpTotal()).isEqualTo(110);
        assertThat(student.getLevel()).isEqualTo(Level.PRATA);
        assertThat(student.getLevelUpEvents()).hasSize(1);

        LevelUpEvent event = student.getLevelUpEvents().get(0);
        assertThat(event.getFromLevel()).isEqualTo(Level.BRONZE);
        assertThat(event.getToLevel()).isEqualTo(Level.PRATA);
    }

    @Test
    @DisplayName("Deve acumular XP sem mudar de nível quando não atingir próxima faixa")
    void deveAcumularXpSemMudarNivel() {
        Student student = new Student("Aluno US2");
        student.receiveXp("LEITURA", 50);

        assertThat(student.getXpTotal()).isEqualTo(50);
        assertThat(student.getLevel()).isEqualTo(Level.BRONZE);
        assertThat(student.getLevelUpEvents()).isEmpty();
    }

    @Test
    @DisplayName("Não deve gerar novo LevelUpEvent se o aluno já estiver no nível máximo (Diamante)")
    void naoDeveGerarNovoLevelUpNoNivelMaximo() {
        Student student = new Student("Aluno US3");
        student.receiveXp("DESAFIO_HARD", 800); // Promove até Diamante (3 eventos: Bronze->Prata, Prata->Ouro, Ouro->Diamante)

        assertThat(student.getLevel()).isEqualTo(Level.DIAMANTE);
        assertThat(student.getLevelUpEvents()).hasSize(3);

        // Recebe mais XP estando em Diamante
        student.receiveXp("DESAFIO_EXTRA", 200);

        assertThat(student.getXpTotal()).isEqualTo(1000);
        assertThat(student.getLevel()).isEqualTo(Level.DIAMANTE);
        assertThat(student.getLevelUpEvents()).hasSize(3); // Mantém apenas os 3 eventos anteriores
    }

    @Test
    @DisplayName("Não deve alterar XP nem gerar eventos ao receber XP menor ou igual a zero")
    void naoDeveProcessarXpInvalido() {
        Student student = new Student("Aluno XP Zero");
        student.receiveXp("MOTIVO_INVALIDO", 0);
        student.receiveXp("MOTIVO_INVALIDO", -10);

        assertThat(student.getXpTotal()).isEqualTo(0);
        assertThat(student.getLevel()).isEqualTo(Level.BRONZE);
        assertThat(student.getLevelUpEvents()).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar XP mínimo correto para cada nível da Enum Level")
    void deveRetornarMinXpDosNiveis() {
        assertThat(Level.BRONZE.getMinXp()).isEqualTo(0);
        assertThat(Level.PRATA.getMinXp()).isEqualTo(100);
        assertThat(Level.OURO.getMinXp()).isEqualTo(300);
        assertThat(Level.DIAMANTE.getMinXp()).isEqualTo(700);
    }
}
package org.example.ac1devops.domain.guilherme;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudentTest {

    private Student student;

    @BeforeEach
    void setUp() {
        // arrange: leva o aluno de Bronze ate Diamante com 4 chamadas reais de receiveXp,
        // gerando exatamente 3 eventos de level-up (Bronze->Prata, Prata->Ouro, Ouro->Diamante)
        student = new Student("Aluno Teste");
        student.receiveXp("SETUP", 50);   // 50  -> BRONZE   (sem evento)
        student.receiveXp("SETUP", 100);  // 150 -> PRATA     (evento 1)
        student.receiveXp("SETUP", 200);  // 350 -> OURO      (evento 2)
        student.receiveXp("SETUP", 400);  // 750 -> DIAMANTE  (evento 3)
    }

    @Test
    void naoDeveGerarNovoLevelUpQuandoAlunoJaEstaNoNivelMaximo() {
        student.receiveXp("ATIVIDADE_CONCLUIDA", 100);

        assertEquals(850, student.getXpTotal());
        assertEquals(Level.DIAMANTE, student.getLevel());
        assertEquals(3, student.getLevelUpHistory().size());
    }
}

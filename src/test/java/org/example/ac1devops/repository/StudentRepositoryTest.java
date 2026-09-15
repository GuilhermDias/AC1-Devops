package org.example.ac1devops.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.example.ac1devops.domain.Level;
import org.example.ac1devops.entity.LevelUpEventEntity;
import org.example.ac1devops.entity.StudentEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void deveSalvarERecarregarAlunoComHistoricoDeLevelUp() {
        StudentEntity entity = new StudentEntity("Aluno Persistido");
        entity.setXpTotal(110);
        entity.setLevel(Level.PRATA);
        entity.addLevelUpEvent(new LevelUpEventEntity(Level.BRONZE, Level.PRATA));

        StudentEntity saved = studentRepository.save(entity);

        Optional<StudentEntity> reloaded = studentRepository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getName()).isEqualTo("Aluno Persistido");
        assertThat(reloaded.get().getXpTotal()).isEqualTo(110);
        assertThat(reloaded.get().getLevel()).isEqualTo(Level.PRATA);
        assertThat(reloaded.get().getLevelUpEvents()).hasSize(1);
        LevelUpEventEntity persistedEvent = reloaded.get().getLevelUpEvents().get(0);
        assertThat(persistedEvent.getId()).isNotNull();
        assertThat(persistedEvent.getFromLevel()).isEqualTo(Level.BRONZE);
        assertThat(persistedEvent.getToLevel()).isEqualTo(Level.PRATA);
        assertThat(persistedEvent.getStudent().getId()).isEqualTo(saved.getId());
    }

    @Test
    void deveRemoverEventosOrfaosAoLimparHistorico() {
        StudentEntity entity = new StudentEntity("Aluno Orfao");
        entity.addLevelUpEvent(new LevelUpEventEntity(Level.BRONZE, Level.PRATA));
        StudentEntity saved = studentRepository.saveAndFlush(entity);

        saved.getLevelUpEvents().clear();
        studentRepository.saveAndFlush(saved);

        StudentEntity reloaded = studentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getLevelUpEvents()).isEmpty();
    }

    @Test
    void deveListarTodosOsAlunosSalvos() {
        studentRepository.save(new StudentEntity("Aluno 1"));
        studentRepository.save(new StudentEntity("Aluno 2"));

        assertThat(studentRepository.findAll()).hasSize(2);
    }
}

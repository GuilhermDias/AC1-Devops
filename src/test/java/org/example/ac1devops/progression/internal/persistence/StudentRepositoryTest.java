package org.example.ac1devops.progression.internal.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.example.ac1devops.progression.api.Level;
import org.example.ac1devops.progression.api.StudentScoreDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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

    @Test
    void deveOrdenarPorXpDecrescenteEDesempatarPeloIdNoBanco() {
        StudentEntity primeiroCom300 = saveWithXp("Primeiro 300", 300, Level.OURO);
        StudentEntity lider = saveWithXp("Lider", 900, Level.DIAMANTE);
        StudentEntity segundoCom300 = saveWithXp("Segundo 300", 300, Level.OURO);
        saveWithXp("Ultimo", 10, Level.BRONZE);

        Page<StudentScoreDTO> first = studentRepository.findScoresOrderedByXp(PageRequest.of(0, 3));
        Page<StudentScoreDTO> second = studentRepository.findScoresOrderedByXp(PageRequest.of(1, 3));

        assertThat(first.getContent()).extracting(StudentScoreDTO::id)
                .containsExactly(lider.getId(), primeiroCom300.getId(), segundoCom300.getId());
        assertThat(first.getContent().get(0))
                .isEqualTo(new StudentScoreDTO(lider.getId(), "Lider", 900, Level.DIAMANTE));
        assertThat(second.getContent()).extracting(StudentScoreDTO::name).containsExactly("Ultimo");
        assertThat(first.getTotalElements()).isEqualTo(4);
        assertThat(first.getTotalPages()).isEqualTo(2);
    }

    @Test
    void deveContarAlunosAFrenteConsiderandoEmpatePeloId() {
        saveWithXp("Lider", 900, Level.DIAMANTE);
        StudentEntity primeiroCom300 = saveWithXp("Primeiro 300", 300, Level.OURO);
        StudentEntity segundoCom300 = saveWithXp("Segundo 300", 300, Level.OURO);

        assertThat(studentRepository.countAhead(300, primeiroCom300.getId())).isEqualTo(1);
        assertThat(studentRepository.countAhead(300, segundoCom300.getId())).isEqualTo(2);
    }

    @Test
    void deveBuscarPontuacaoPorId() {
        StudentEntity saved = saveWithXp("Aluno Score", 150, Level.PRATA);

        assertThat(studentRepository.findScoreById(saved.getId()))
                .contains(new StudentScoreDTO(saved.getId(), "Aluno Score", 150, Level.PRATA));
        assertThat(studentRepository.findScoreById(-1L)).isEmpty();
    }

    private StudentEntity saveWithXp(String name, int xpTotal, Level level) {
        StudentEntity entity = new StudentEntity(name);
        entity.setXpTotal(xpTotal);
        entity.setLevel(level);
        return studentRepository.save(entity);
    }
}

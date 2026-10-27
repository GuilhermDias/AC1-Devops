package org.example.ac1devops.progression.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.example.ac1devops.progression.api.CreateStudentRequestDTO;
import org.example.ac1devops.progression.api.Level;
import org.example.ac1devops.progression.api.LevelUpEventDTO;
import org.example.ac1devops.progression.api.ReceiveXpRequestDTO;
import org.example.ac1devops.progression.api.StudentNotFoundException;
import org.example.ac1devops.progression.api.StudentResponseDTO;
import org.example.ac1devops.progression.api.StudentScoreChangedEvent;
import org.example.ac1devops.progression.api.StudentScoreDTO;
import org.example.ac1devops.progression.internal.persistence.LevelUpEventEntity;
import org.example.ac1devops.progression.internal.persistence.StudentEntity;
import org.example.ac1devops.progression.internal.persistence.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository, eventPublisher);
    }

    @Test
    void deveCriarAlunoComXpZeroENivelBronze() {
        when(studentRepository.save(any(StudentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponseDTO response = studentService.createStudent(new CreateStudentRequestDTO("Aluno Teste"));

        assertThat(response.name()).isEqualTo("Aluno Teste");
        assertThat(response.xpTotal()).isEqualTo(0);
        assertThat(response.level()).isEqualTo(Level.BRONZE);
        assertThat(response.levelUpEvents()).isEmpty();
    }

    @Test
    void deveAcumularXpEGerarLevelUpAoCruzarFaixa() {
        StudentEntity entity = new StudentEntity("Aluno US1");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(studentRepository.save(any(StudentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponseDTO response = studentService.receiveXp(1L, new ReceiveXpRequestDTO("ATIVIDADE_CONCLUIDA", 110));

        assertThat(response.xpTotal()).isEqualTo(110);
        assertThat(response.level()).isEqualTo(Level.PRATA);
        assertThat(response.levelUpEvents()).hasSize(1);
        assertThat(response.levelUpEvents().get(0).fromLevel()).isEqualTo(Level.BRONZE);
        assertThat(response.levelUpEvents().get(0).toLevel()).isEqualTo(Level.PRATA);
    }

    @Test
    void deveGerarVariosLevelUpsAoCruzarVariasFaixasDeUmaVez() {
        StudentEntity entity = new StudentEntity("Aluno US3");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(studentRepository.save(any(StudentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponseDTO response = studentService.receiveXp(1L, new ReceiveXpRequestDTO("DESAFIO_HARD", 800));

        assertThat(response.level()).isEqualTo(Level.DIAMANTE);
        assertThat(response.levelUpEvents()).hasSize(3);
    }

    @Test
    void naoDeveGerarNovoEventoAoReceberXpNoNivelMaximo() {
        StudentEntity entity = new StudentEntity("Aluno Diamante");
        when(studentRepository.findById(1L))
                .thenReturn(Optional.of(entity))
                .thenReturn(Optional.of(entity));
        when(studentRepository.save(any(StudentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studentService.receiveXp(1L, new ReceiveXpRequestDTO("DESAFIO_HARD", 800));
        StudentResponseDTO response = studentService.receiveXp(1L, new ReceiveXpRequestDTO("DESAFIO_EXTRA", 200));

        assertThat(response.xpTotal()).isEqualTo(1000);
        assertThat(response.level()).isEqualTo(Level.DIAMANTE);
        assertThat(response.levelUpEvents()).hasSize(3);
    }

    @Test
    void deveLancarExcecaoAoReceberXpParaAlunoInexistente() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.receiveXp(99L, new ReceiveXpRequestDTO("X", 10)))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void deveLancarExcecaoAoConsultarAlunoInexistente() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudent(99L))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void deveRetornarAlunoPorId() {
        StudentEntity entity = new StudentEntity("Aluno Consulta");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(entity));

        StudentResponseDTO response = studentService.getStudent(1L);

        assertThat(response.name()).isEqualTo("Aluno Consulta");
    }

    @Test
    void deveListarTodosOsAlunos() {
        when(studentRepository.findAll())
                .thenReturn(List.of(new StudentEntity("Aluno 1"), new StudentEntity("Aluno 2")));

        List<StudentResponseDTO> response = studentService.listStudents();

        assertThat(response).hasSize(2);
    }

    @Test
    void deveReconstruirEstadoPersistidoEPersistirSoOsEventosNovos() {
        StudentEntity entity = new StudentEntity("Aluno Persistido");
        entity.setXpTotal(150);
        entity.setLevel(Level.PRATA);
        entity.addLevelUpEvent(new LevelUpEventEntity(Level.BRONZE, Level.PRATA));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(studentRepository.save(any(StudentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponseDTO response = studentService.receiveXp(1L, new ReceiveXpRequestDTO("ATIVIDADE_CONCLUIDA", 200));

        assertThat(response.xpTotal()).isEqualTo(350);
        assertThat(response.level()).isEqualTo(Level.OURO);
        assertThat(response.levelUpEvents()).containsExactly(
                new LevelUpEventDTO(Level.BRONZE, Level.PRATA),
                new LevelUpEventDTO(Level.PRATA, Level.OURO));
    }

    @Test
    void deveIgnorarXpInvalidoMantendoEstadoDoAluno() {
        StudentEntity entity = new StudentEntity("Aluno XP Invalido");
        entity.setXpTotal(50);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(studentRepository.save(any(StudentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponseDTO response = studentService.receiveXp(1L, new ReceiveXpRequestDTO("MOTIVO_INVALIDO", -10));

        assertThat(response.xpTotal()).isEqualTo(50);
        assertThat(response.level()).isEqualTo(Level.BRONZE);
        assertThat(response.levelUpEvents()).isEmpty();
        verify(studentRepository).save(entity);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void devePublicarEventoDePontuacaoAoCriarAluno() {
        when(studentRepository.save(any(StudentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studentService.createStudent(new CreateStudentRequestDTO("Aluno Novo"));

        verify(eventPublisher).publishEvent(new StudentScoreChangedEvent(null, 0));
    }

    @Test
    void devePublicarEventoDePontuacaoQuandoXpMuda() {
        StudentEntity entity = new StudentEntity("Aluno Evento");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(studentRepository.save(any(StudentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studentService.receiveXp(1L, new ReceiveXpRequestDTO("ATIVIDADE_CONCLUIDA", 40));

        verify(eventPublisher).publishEvent(new StudentScoreChangedEvent(null, 40));
    }

    @Test
    void deveListarPontuacaoPaginadaIgnorandoOrdenacaoRecebida() {
        Page<StudentScoreDTO> page = new PageImpl<>(List.of(new StudentScoreDTO(1L, "Aluno 1", 500, Level.OURO)));
        when(studentRepository.findScoresOrderedByXp(PageRequest.of(2, 10))).thenReturn(page);

        Page<StudentScoreDTO> response = studentService.listByScore(PageRequest.of(2, 10, Sort.by("name")));

        assertThat(response).isSameAs(page);
    }

    @Test
    void deveRetornarPontuacaoDoAluno() {
        StudentScoreDTO score = new StudentScoreDTO(1L, "Aluno 1", 500, Level.OURO);
        when(studentRepository.findScoreById(1L)).thenReturn(Optional.of(score));

        assertThat(studentService.getScore(1L)).isEqualTo(score);
    }

    @Test
    void deveLancarExcecaoAoConsultarPontuacaoDeAlunoInexistente() {
        when(studentRepository.findScoreById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getScore(99L))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void deveContarAlunosAFrente() {
        when(studentRepository.countAhead(500, 7L)).thenReturn(3L);

        assertThat(studentService.countStudentsAhead(new StudentScoreDTO(7L, "Aluno 7", 500, Level.OURO)))
                .isEqualTo(3L);
    }
}

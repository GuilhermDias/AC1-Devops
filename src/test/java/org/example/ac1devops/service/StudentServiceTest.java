package org.example.ac1devops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.example.ac1devops.domain.Level;
import org.example.ac1devops.dto.CreateStudentRequestDTO;
import org.example.ac1devops.dto.ReceiveXpRequestDTO;
import org.example.ac1devops.dto.StudentResponseDTO;
import org.example.ac1devops.entity.StudentEntity;
import org.example.ac1devops.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository);
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
}

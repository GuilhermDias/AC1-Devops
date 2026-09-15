package org.example.ac1devops.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import java.util.List;
import org.example.ac1devops.domain.Level;
import org.example.ac1devops.dto.CreateStudentRequestDTO;
import org.example.ac1devops.dto.LevelUpEventDTO;
import org.example.ac1devops.dto.ReceiveXpRequestDTO;
import org.example.ac1devops.dto.StudentResponseDTO;
import org.example.ac1devops.service.StudentNotFoundException;
import org.example.ac1devops.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    private MockMvcTester mvc;

    @BeforeEach
    void setUp() {
        mvc = create(mockMvc);
    }

    @Test
    void devePostarNovoAlunoERetornar201() {
        when(studentService.createStudent(any())).thenReturn(
                new StudentResponseDTO(1L, "Aluno Teste", 0, Level.BRONZE, List.of()));

        mvc.post().uri("/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Aluno Teste\"}")
                .exchange()
                .assertThat()
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Aluno Teste");
    }

    @Test
    void deveReceberXpERetornarAlunoAtualizado() {
        when(studentService.receiveXp(eq(1L), any())).thenReturn(
                new StudentResponseDTO(1L, "Aluno US1", 110, Level.PRATA,
                        List.of(new LevelUpEventDTO(Level.BRONZE, Level.PRATA))));

        mvc.post().uri("/students/{id}/xp", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"ATIVIDADE_CONCLUIDA\",\"amount\":110}")
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.level")
                .isEqualTo("PRATA");
    }

    @Test
    void deveRetornar404AoReceberXpParaAlunoInexistente() {
        when(studentService.receiveXp(eq(99L), any())).thenThrow(new StudentNotFoundException(99L));

        mvc.post().uri("/students/{id}/xp", 99L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"X\",\"amount\":10}")
                .exchange()
                .assertThat()
                .hasStatus(404);
    }

    @Test
    void deveConsultarAlunoPorId() {
        when(studentService.getStudent(1L)).thenReturn(
                new StudentResponseDTO(1L, "Aluno Consulta", 50, Level.BRONZE, List.of()));

        mvc.get().uri("/students/{id}", 1L)
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Aluno Consulta");
    }

    @Test
    void deveRetornar404AoConsultarAlunoInexistente() {
        when(studentService.getStudent(99L)).thenThrow(new StudentNotFoundException(99L));

        mvc.get().uri("/students/{id}", 99L)
                .exchange()
                .assertThat()
                .hasStatus(404);
    }

    @Test
    void deveListarTodosOsAlunos() {
        when(studentService.listStudents()).thenReturn(List.of(
                new StudentResponseDTO(1L, "Aluno 1", 0, Level.BRONZE, List.of()),
                new StudentResponseDTO(2L, "Aluno 2", 0, Level.BRONZE, List.of())));

        mvc.get().uri("/students")
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.length()")
                .isEqualTo(2);
    }
}

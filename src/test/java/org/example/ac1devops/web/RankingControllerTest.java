package org.example.ac1devops.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import java.util.List;
import org.example.ac1devops.progression.api.Level;
import org.example.ac1devops.progression.api.StudentNotFoundException;
import org.example.ac1devops.ranking.api.InvalidRankingQueryException;
import org.example.ac1devops.ranking.api.RankingApi;
import org.example.ac1devops.ranking.api.RankingEntryDTO;
import org.example.ac1devops.ranking.api.RankingPageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(RankingController.class)
class RankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RankingApi ranking;

    private MockMvcTester mvc;

    @BeforeEach
    void setUp() {
        mvc = create(mockMvc);
    }

    @Test
    void deveRetornarPaginaDoRankingComValoresPadrao() {
        when(ranking.getRanking(0, 20)).thenReturn(new RankingPageDTO(
                List.of(new RankingEntryDTO(1, 3L, "Lider", 900, Level.DIAMANTE)), 0, 20, 1, 1));

        mvc.get().uri("/ranking")
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.entries[0].name")
                .isEqualTo("Lider");
    }

    @Test
    void deveRetornarTopN() {
        when(ranking.getTop(3)).thenReturn(List.of(new RankingEntryDTO(1, 3L, "Lider", 900, Level.DIAMANTE)));

        mvc.get().uri("/ranking/top?n=3")
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].position")
                .isEqualTo(1);
    }

    @Test
    void deveRetornarPosicaoDoAluno() {
        when(ranking.getPosition(7L)).thenReturn(new RankingEntryDTO(42, 7L, "Aluno 7", 300, Level.OURO));

        mvc.get().uri("/ranking/students/{id}", 7L)
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.position")
                .isEqualTo(42);
    }

    @Test
    void deveRetornar400ParaParametrosInvalidos() {
        when(ranking.getRanking(0, 500)).thenThrow(new InvalidRankingQueryException("size deve estar entre 1 e 100"));

        mvc.get().uri("/ranking?size=500")
                .exchange()
                .assertThat()
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("size deve estar entre 1 e 100");
    }

    @Test
    void deveRetornar404ParaAlunoInexistente() {
        when(ranking.getPosition(99L)).thenThrow(new StudentNotFoundException(99L));

        mvc.get().uri("/ranking/students/{id}", 99L)
                .exchange()
                .assertThat()
                .hasStatus(404);
    }
}

package org.example.ac1devops.ranking.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.example.ac1devops.progression.api.Level;
import org.example.ac1devops.progression.api.StudentNotFoundException;
import org.example.ac1devops.progression.api.StudentProgressionApi;
import org.example.ac1devops.progression.api.StudentScoreDTO;
import org.example.ac1devops.ranking.api.InvalidRankingQueryException;
import org.example.ac1devops.ranking.api.RankingEntryDTO;
import org.example.ac1devops.ranking.api.RankingPageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private StudentProgressionApi studentProgression;

    private RankingService rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new RankingService(studentProgression);
    }

    @Test
    void deveCalcularPosicaoAPartirDoDeslocamentoDaPagina() {
        PageRequest pageable = PageRequest.of(2, 2);
        when(studentProgression.listByScore(pageable)).thenReturn(new PageImpl<>(List.of(
                new StudentScoreDTO(10L, "Aluno A", 400, Level.OURO),
                new StudentScoreDTO(11L, "Aluno B", 350, Level.OURO)), pageable, 7));

        RankingPageDTO response = rankingService.getRanking(2, 2);

        assertThat(response.entries()).containsExactly(
                new RankingEntryDTO(5, 10L, "Aluno A", 400, Level.OURO),
                new RankingEntryDTO(6, 11L, "Aluno B", 350, Level.OURO));
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(7);
        assertThat(response.totalPages()).isEqualTo(4);
    }

    @Test
    void deveRetornarTopNAPartirDaPrimeiraPagina() {
        PageRequest pageable = PageRequest.of(0, 3);
        when(studentProgression.listByScore(pageable)).thenReturn(new PageImpl<>(List.of(
                new StudentScoreDTO(1L, "Lider", 900, Level.DIAMANTE)), pageable, 1));

        assertThat(rankingService.getTop(3))
                .containsExactly(new RankingEntryDTO(1, 1L, "Lider", 900, Level.DIAMANTE));
    }

    @Test
    void deveCalcularPosicaoDoAlunoContandoQuemEstaAFrente() {
        StudentScoreDTO score = new StudentScoreDTO(7L, "Aluno 7", 300, Level.OURO);
        when(studentProgression.getScore(7L)).thenReturn(score);
        when(studentProgression.countStudentsAhead(score)).thenReturn(41L);

        assertThat(rankingService.getPosition(7L))
                .isEqualTo(new RankingEntryDTO(42, 7L, "Aluno 7", 300, Level.OURO));
    }

    @Test
    void devePropagarAlunoInexistenteAoConsultarPosicao() {
        when(studentProgression.getScore(99L)).thenThrow(new StudentNotFoundException(99L));

        assertThatThrownBy(() -> rankingService.getPosition(99L)).isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void deveRejeitarParametrosForaDosLimitesSemConsultarOBanco() {
        assertThatThrownBy(() -> rankingService.getRanking(-1, 20))
                .isInstanceOf(InvalidRankingQueryException.class).hasMessage("page deve ser >= 0");
        assertThatThrownBy(() -> rankingService.getRanking(0, 0))
                .isInstanceOf(InvalidRankingQueryException.class).hasMessage("size deve estar entre 1 e 100");
        assertThatThrownBy(() -> rankingService.getRanking(0, 101))
                .isInstanceOf(InvalidRankingQueryException.class);
        assertThatThrownBy(() -> rankingService.getTop(0))
                .isInstanceOf(InvalidRankingQueryException.class).hasMessage("n deve estar entre 1 e 100");
        assertThatThrownBy(() -> rankingService.getTop(101))
                .isInstanceOf(InvalidRankingQueryException.class);
        verifyNoInteractions(studentProgression);
    }
}

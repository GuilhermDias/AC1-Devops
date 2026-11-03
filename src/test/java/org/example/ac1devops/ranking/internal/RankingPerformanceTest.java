package org.example.ac1devops.ranking.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.example.ac1devops.progression.api.Level;
import org.example.ac1devops.progression.internal.StudentService;
import org.example.ac1devops.ranking.api.RankingApi;
import org.example.ac1devops.ranking.api.RankingEntryDTO;
import org.example.ac1devops.ranking.api.RankingPageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Ranking com 100.000 alunos (RNF de desempenho, ASR2). Banco H2 proprio do {@code @DataJpaTest},
 * populado em lote via JDBC dentro da transacao do teste (rollback ao final). Mede a consulta
 * paginada real (StudentService -> StudentRepository -> banco), sem cache.
 *
 * <p>XP de cada aluno e {@code (i * 7919) % 100000}: uma permutacao de 0..99999, entao o lider tem
 * 99999 XP, o segundo 99998, e assim por diante.
 */
@DataJpaTest
@Import({StudentService.class, RankingService.class})
class RankingPerformanceTest {

    private static final int STUDENTS = 100_000;
    private static final int BATCH_SIZE = 5_000;
    private static final long MAX_MILLIS = 1_000;

    @Autowired
    private RankingApi ranking;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long insertMillis;

    @BeforeEach
    void populate() {
        long start = System.nanoTime();
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        for (int i = 0; i < STUDENTS; i++) {
            int xp = (int) ((i * 7919L) % STUDENTS);
            batch.add(new Object[] {"Aluno " + i, xp, Level.fromXp(xp).name()});
            if (batch.size() == BATCH_SIZE) {
                jdbcTemplate.batchUpdate("insert into students (name, xp_total, level) values (?, ?, ?)", batch);
                batch.clear();
            }
        }
        insertMillis = (System.nanoTime() - start) / 1_000_000;
    }

    @Test
    void deveResponderConsultaPaginadaCom100MilAlunosNoBanco() {
        assertThat(jdbcTemplate.queryForObject("select count(*) from students", Long.class)).isEqualTo(STUDENTS);

        Timed<RankingPageDTO> firstPage = measure(() -> ranking.getRanking(0, 20));
        Timed<RankingPageDTO> deepPage = measure(() -> ranking.getRanking(4_999, 20));
        Long lastId = jdbcTemplate.queryForObject("select id from students where xp_total = 0", Long.class);
        Timed<RankingEntryDTO> position = measure(() -> ranking.getPosition(lastId));

        assertThat(firstPage.value().totalElements()).isEqualTo(STUDENTS);
        assertThat(firstPage.value().totalPages()).isEqualTo(5_000);
        assertThat(firstPage.value().entries()).hasSize(20);
        assertThat(firstPage.value().entries().get(0).position()).isEqualTo(1);
        assertThat(firstPage.value().entries().get(0).xpTotal()).isEqualTo(99_999);
        assertThat(firstPage.value().entries().get(0).level()).isEqualTo(Level.DIAMANTE);
        assertThat(firstPage.value().entries()).extracting(RankingEntryDTO::xpTotal)
                .isSortedAccordingTo((a, b) -> Integer.compare(b, a));

        assertThat(deepPage.value().entries()).hasSize(20);
        assertThat(deepPage.value().entries().get(0).position()).isEqualTo(99_981);
        assertThat(deepPage.value().entries().get(19).xpTotal()).isZero();

        assertThat(position.value().position()).isEqualTo(STUDENTS);

        System.out.printf("[RankingPerformanceTest] carga de %d alunos em lote: %d ms%n", STUDENTS, insertMillis);
        System.out.printf("[RankingPerformanceTest] pagina 0 (size 20): %d ms%n", firstPage.millis());
        System.out.printf("[RankingPerformanceTest] pagina 4999 (size 20, ultimas posicoes): %d ms%n", deepPage.millis());
        System.out.printf("[RankingPerformanceTest] posicao de um aluno: %d ms%n", position.millis());

        assertThat(firstPage.millis()).isLessThan(MAX_MILLIS);
        assertThat(deepPage.millis()).isLessThan(MAX_MILLIS);
        assertThat(position.millis()).isLessThan(MAX_MILLIS);
    }

    @Test
    void deveUsarOIndiceDeXpParaOrdenarNoBanco() {
        String plan = jdbcTemplate.queryForObject(
                "explain select id from students order by xp_total desc, id asc limit 20", String.class);

        System.out.printf("[RankingPerformanceTest] plano H2: %s%n", plan.replaceAll("\\s+", " "));
        assertThat(plan).containsIgnoringCase("IDX_STUDENTS_XP_TOTAL");
        assertThat(plan).containsIgnoringCase("index sorted");
    }

    /** Media de 5 execucoes depois de 1 execucao de aquecimento. */
    private static <T> Timed<T> measure(Supplier<T> query) {
        T value = query.get();
        int runs = 5;
        long start = System.nanoTime();
        for (int i = 0; i < runs; i++) {
            value = query.get();
        }
        return new Timed<>(value, (System.nanoTime() - start) / 1_000_000 / runs);
    }

    private record Timed<T>(T value, long millis) {
    }
}

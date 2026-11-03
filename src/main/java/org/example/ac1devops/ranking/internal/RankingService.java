package org.example.ac1devops.ranking.internal;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.stream.IntStream;
import org.example.ac1devops.progression.api.StudentProgressionApi;
import org.example.ac1devops.progression.api.StudentScoreDTO;
import org.example.ac1devops.ranking.api.InvalidRankingQueryException;
import org.example.ac1devops.ranking.api.RankingApi;
import org.example.ac1devops.ranking.api.RankingEntryDTO;
import org.example.ac1devops.ranking.api.RankingPageDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Implementacao de {@link RankingApi}. Nao guarda dados proprios: le a pontuacao pela
 * {@link StudentProgressionApi}, que ordena e pagina no banco, e so calcula a posicao de cada
 * linha a partir do deslocamento da pagina.
 */
@Service
public class RankingService implements RankingApi {

    private static final Timer PAGE_TIMER = queryTimer("page");
    private static final Timer TOP_TIMER = queryTimer("top");
    private static final Timer POSITION_TIMER = queryTimer("position");

    private final StudentProgressionApi studentProgression;

    public RankingService(StudentProgressionApi studentProgression) {
        this.studentProgression = studentProgression;
    }

    @Override
    public RankingPageDTO getRanking(int page, int size) {
        if (page < 0) {
            throw new InvalidRankingQueryException("page deve ser >= 0");
        }
        requireSize("size", size);
        return PAGE_TIMER.record(() -> {
            Page<StudentScoreDTO> scores = studentProgression.listByScore(PageRequest.of(page, size));
            return new RankingPageDTO(toEntries(scores), page, size,
                    scores.getTotalElements(), scores.getTotalPages());
        });
    }

    @Override
    @Cacheable(cacheNames = RankingCacheConfig.TOP_CACHE, key = "#n")
    public List<RankingEntryDTO> getTop(int n) {
        requireSize("n", n);
        return TOP_TIMER.record(() -> toEntries(studentProgression.listByScore(PageRequest.of(0, n))));
    }

    @Override
    public RankingEntryDTO getPosition(Long studentId) {
        return POSITION_TIMER.record(() -> {
            StudentScoreDTO score = studentProgression.getScore(studentId);
            return toEntry(studentProgression.countStudentsAhead(score) + 1, score);
        });
    }

    /** {@code ranking_query_seconds}: valida o p95 da ASR2. O top em cache so conta quando vai ao banco. */
    private static Timer queryTimer(String operation) {
        return Timer.builder("ranking.query")
                .tag("operation", operation)
                .publishPercentileHistogram()
                .register(Metrics.globalRegistry);
    }

    private static void requireSize(String name, int value) {
        if (value < 1 || value > MAX_PAGE_SIZE) {
            throw new InvalidRankingQueryException(name + " deve estar entre 1 e " + MAX_PAGE_SIZE);
        }
    }

    private static List<RankingEntryDTO> toEntries(Page<StudentScoreDTO> scores) {
        long firstPosition = scores.getPageable().getOffset() + 1;
        List<StudentScoreDTO> content = scores.getContent();
        return IntStream.range(0, content.size())
                .mapToObj(i -> toEntry(firstPosition + i, content.get(i)))
                .toList();
    }

    private static RankingEntryDTO toEntry(long position, StudentScoreDTO score) {
        return new RankingEntryDTO(position, score.id(), score.name(), score.xpTotal(), score.level());
    }
}

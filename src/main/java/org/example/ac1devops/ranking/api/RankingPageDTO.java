package org.example.ac1devops.ranking.api;

import java.util.List;

public record RankingPageDTO(
        List<RankingEntryDTO> entries,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}

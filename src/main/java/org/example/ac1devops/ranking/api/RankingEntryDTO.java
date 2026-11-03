package org.example.ac1devops.ranking.api;

import org.example.ac1devops.progression.api.Level;

public record RankingEntryDTO(long position, Long studentId, String name, int xpTotal, Level level) {
}

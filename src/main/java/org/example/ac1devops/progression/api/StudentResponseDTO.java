package org.example.ac1devops.progression.api;

import java.util.List;

public record StudentResponseDTO(
        Long id,
        String name,
        int xpTotal,
        Level level,
        List<LevelUpEventDTO> levelUpEvents) {
}

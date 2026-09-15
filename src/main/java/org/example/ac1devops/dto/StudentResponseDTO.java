package org.example.ac1devops.dto;

import java.util.List;
import org.example.ac1devops.domain.Level;

public record StudentResponseDTO(
        Long id,
        String name,
        int xpTotal,
        Level level,
        List<LevelUpEventDTO> levelUpEvents) {
}

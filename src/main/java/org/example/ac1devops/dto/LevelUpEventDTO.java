package org.example.ac1devops.dto;

import org.example.ac1devops.domain.Level;

public record LevelUpEventDTO(Level fromLevel, Level toLevel) {
}

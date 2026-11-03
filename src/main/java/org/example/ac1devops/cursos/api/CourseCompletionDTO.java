package org.example.ac1devops.cursos.api;

import org.example.ac1devops.progression.api.Level;

public record CourseCompletionDTO(
        Long completionId,
        Long studentId,
        String courseId,
        int xpAwarded,
        int xpTotal,
        Level level,
        boolean alreadyCompleted) {
}

package org.example.ac1devops.cursos.api;

public record CourseEventDTO(
        Long id,
        Long studentId,
        String courseId,
        String status,
        int attempts,
        String lastError) {
}

package org.example.ac1devops.web;

import java.util.Map;
import org.example.ac1devops.cursos.api.CourseEventNotFoundException;
import org.example.ac1devops.cursos.api.CourseNotFoundException;
import org.example.ac1devops.progression.api.StudentNotFoundException;
import org.example.ac1devops.progression.api.StudentUpdateConflictException;
import org.example.ac1devops.ranking.api.InvalidRankingQueryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({StudentNotFoundException.class, CourseNotFoundException.class, CourseEventNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(StudentUpdateConflictException.class)
    public ResponseEntity<Map<String, String>> handleUpdateConflict(StudentUpdateConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(InvalidRankingQueryException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRankingQuery(InvalidRankingQueryException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}

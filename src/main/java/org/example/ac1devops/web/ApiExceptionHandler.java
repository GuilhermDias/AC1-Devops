package org.example.ac1devops.web;

import java.util.Map;
import org.example.ac1devops.progression.api.StudentNotFoundException;
import org.example.ac1devops.ranking.api.InvalidRankingQueryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleStudentNotFound(StudentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(InvalidRankingQueryException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRankingQuery(InvalidRankingQueryException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}

package org.example.ac1devops.progression.api;

public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(Long id) {
        super("Aluno nao encontrado: " + id);
    }
}

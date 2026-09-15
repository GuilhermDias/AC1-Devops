package org.example.ac1devops.service;

public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(Long id) {
        super("Aluno nao encontrado: " + id);
    }
}

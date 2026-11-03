package org.example.ac1devops.progression.api;

public class StudentUpdateConflictException extends RuntimeException {

    public StudentUpdateConflictException(Long id, Throwable cause) {
        super("Conflito ao atualizar o aluno " + id + ": tente novamente", cause);
    }
}

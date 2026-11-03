package org.example.ac1devops.iot.internal.domain;

/** O evento e bem formado, mas aponta para um aluno ou atividade que nao existe. */
public class UnknownActivityReferenceException extends RuntimeException {

    public UnknownActivityReferenceException(String message) {
        super(message);
    }
}

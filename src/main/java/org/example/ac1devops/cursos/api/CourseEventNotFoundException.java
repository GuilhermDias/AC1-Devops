package org.example.ac1devops.cursos.api;

public class CourseEventNotFoundException extends RuntimeException {

    public CourseEventNotFoundException(Long eventId) {
        super("Evento com tentativas esgotadas nao encontrado: " + eventId);
    }
}

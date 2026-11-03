package org.example.ac1devops.cursos.api;

public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(String courseId) {
        super("Curso nao encontrado: " + courseId);
    }
}

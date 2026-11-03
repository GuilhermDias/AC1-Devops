package org.example.ac1devops.cursos.internal.domain;

import java.util.List;
import java.util.Optional;

/**
 * Cursos e recompensas vigentes. Enquanto nao ha gestao de cursos, e uma lista fixa proposta pela
 * equipe, com os mesmos identificadores do catalogo do servico de IA.
 */
public final class CourseCatalog {

    private static final List<Course> COURSES = List.of(
            new Course("fundamentos-git", "Fundamentos de Git", 50),
            new Course("logica-programacao", "Logica de Programacao", 50),
            new Course("testes-unitarios", "Testes Unitarios", 100),
            new Course("docker-essencial", "Docker Essencial", 100),
            new Course("ci-cd", "Integracao e Entrega Continuas", 150),
            new Course("arquitetura-software", "Arquitetura de Software", 150),
            new Course("observabilidade", "Observabilidade", 200),
            new Course("sistemas-distribuidos", "Sistemas Distribuidos", 200));

    private CourseCatalog() {
    }

    public static Optional<Course> find(String courseId) {
        return COURSES.stream().filter(course -> course.courseId().equals(courseId)).findFirst();
    }
}

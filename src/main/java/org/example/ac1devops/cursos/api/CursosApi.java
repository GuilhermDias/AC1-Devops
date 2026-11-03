package org.example.ac1devops.cursos.api;

import java.util.List;

/**
 * Fachada publica do modulo de cursos. Concluir um curso e sincrono e atomico: conclusao, recompensa
 * e registro do evento entram na mesma transacao. A entrega do {@link CursoConcluidoEvent} acontece
 * depois do commit e nao afeta a resposta.
 */
public interface CursosApi {

    /**
     * Conclui o curso e concede a recompensa uma unica vez. Repetir a chamada nao concede XP de novo.
     *
     * @throws CourseNotFoundException se o curso nao existir
     * @throws org.example.ac1devops.progression.api.StudentNotFoundException se o aluno nao existir
     * @throws org.example.ac1devops.progression.api.StudentUpdateConflictException se as tentativas
     *         acabarem por disputa com outras requisicoes; nesse caso nada foi gravado
     */
    CourseCompletionDTO completeCourse(String courseId, Long studentId);

    /** Os eventos mais recentes, com o estado da entrega. */
    List<CourseEventDTO> listEvents();

    /**
     * Devolve para a fila um evento que esgotou as tentativas.
     *
     * @throws CourseEventNotFoundException se nao houver evento nesse estado com o {@code id} informado
     */
    void requeueEvent(Long eventId);
}

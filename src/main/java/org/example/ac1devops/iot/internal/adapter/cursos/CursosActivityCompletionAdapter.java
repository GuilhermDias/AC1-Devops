package org.example.ac1devops.iot.internal.adapter.cursos;

import org.example.ac1devops.cursos.api.CourseNotFoundException;
import org.example.ac1devops.cursos.api.CursosApi;
import org.example.ac1devops.iot.internal.domain.ActivityCompletionPort;
import org.example.ac1devops.iot.internal.domain.UnknownActivityReferenceException;
import org.example.ac1devops.progression.api.StudentNotFoundException;
import org.springframework.stereotype.Component;

/** Reaproveita o fluxo de conclusao do Desafio 4: recompensa na transacao e notificacao pela outbox. */
@Component
public class CursosActivityCompletionAdapter implements ActivityCompletionPort {

    private final CursosApi cursos;

    public CursosActivityCompletionAdapter(CursosApi cursos) {
        this.cursos = cursos;
    }

    @Override
    public void complete(String activityId, Long studentId) {
        try {
            cursos.completeCourse(activityId, studentId);
        } catch (CourseNotFoundException | StudentNotFoundException unknown) {
            throw new UnknownActivityReferenceException(unknown.getMessage());
        }
    }
}

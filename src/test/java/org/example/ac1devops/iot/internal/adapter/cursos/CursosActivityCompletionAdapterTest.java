package org.example.ac1devops.iot.internal.adapter.cursos;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.ac1devops.cursos.api.CourseNotFoundException;
import org.example.ac1devops.cursos.api.CursosApi;
import org.example.ac1devops.iot.internal.domain.UnknownActivityReferenceException;
import org.example.ac1devops.progression.api.StudentNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CursosActivityCompletionAdapterTest {

    @Mock
    private CursosApi cursos;

    @Test
    void deveReaproveitarOFluxoDeConclusao() {
        new CursosActivityCompletionAdapter(cursos).complete("ci-cd", 7L);

        verify(cursos).completeCourse("ci-cd", 7L);
    }

    @Test
    void cursoOuAlunoInexistenteDevemVirarReferenciaDesconhecida() {
        CursosActivityCompletionAdapter adapter = new CursosActivityCompletionAdapter(cursos);
        when(cursos.completeCourse("x", 7L)).thenThrow(new CourseNotFoundException("x"));
        when(cursos.completeCourse("ci-cd", 9L)).thenThrow(new StudentNotFoundException(9L));

        assertThatThrownBy(() -> adapter.complete("x", 7L)).isInstanceOf(UnknownActivityReferenceException.class);
        assertThatThrownBy(() -> adapter.complete("ci-cd", 9L)).isInstanceOf(UnknownActivityReferenceException.class);
    }
}

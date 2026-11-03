package org.example.ac1devops.cursos.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.ac1devops.cursos.internal.persistence.CourseCompletionRepository;
import org.example.ac1devops.cursos.internal.persistence.OutboxEventRepository;
import org.example.ac1devops.progression.api.StudentProgressionApi;
import org.example.ac1devops.progression.api.StudentUpdateConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class CourseCompletionServiceTest {

    @Mock
    private CourseCompletionRepository completions;

    @Mock
    private OutboxEventRepository outbox;

    @Mock
    private StudentProgressionApi studentProgression;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Test
    void deveDesistirSemConcederXpQuandoAsTentativasAcabam() {
        when(completions.findByStudentIdAndCourseId(any(), any()))
                .thenThrow(new OptimisticLockingFailureException("conflito"));
        CourseCompletionService service = new CourseCompletionService(completions, outbox, studentProgression,
                eventPublisher, new TransactionTemplate(transactionManager));

        assertThatThrownBy(() -> service.completeCourse("ci-cd", 1L))
                .isInstanceOf(StudentUpdateConflictException.class);

        verify(completions, times(CourseCompletionService.MAX_ATTEMPTS)).findByStudentIdAndCourseId(any(), any());
        verify(studentProgression, times(0)).grantXpInCurrentTransaction(any(), any());
    }
}

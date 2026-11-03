package org.example.ac1devops.cursos.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.example.ac1devops.cursos.api.CourseCompletionDTO;
import org.example.ac1devops.cursos.api.CourseEventDTO;
import org.example.ac1devops.cursos.api.CourseEventNotFoundException;
import org.example.ac1devops.cursos.api.CourseNotFoundException;
import org.example.ac1devops.cursos.api.CursosApi;
import org.example.ac1devops.cursos.internal.domain.Course;
import org.example.ac1devops.cursos.internal.domain.CourseCatalog;
import org.example.ac1devops.cursos.internal.persistence.CourseCompletionEntity;
import org.example.ac1devops.cursos.internal.persistence.CourseCompletionRepository;
import org.example.ac1devops.cursos.internal.persistence.OutboxEventEntity;
import org.example.ac1devops.cursos.internal.persistence.OutboxEventRepository;
import org.example.ac1devops.progression.api.ReceiveXpRequestDTO;
import org.example.ac1devops.progression.api.StudentProgressionApi;
import org.example.ac1devops.progression.api.StudentResponseDTO;
import org.example.ac1devops.progression.api.StudentScoreDTO;
import org.example.ac1devops.progression.api.StudentUpdateConflictException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Implementacao de {@link CursosApi}. Conclusao, recompensa e evento sao gravados em uma unica
 * transacao, e toda a transacao e repetida quando ha disputa: na proxima tentativa, uma conclusao
 * gravada por outra requisicao e encontrada e devolvida, sem conceder XP de novo.
 */
@Service
public class CourseCompletionService implements CursosApi {

    static final int MAX_ATTEMPTS = 15;
    private static final long BACKOFF_STEP_MILLIS = 20;

    private final CourseCompletionRepository completions;
    private final OutboxEventRepository outbox;
    private final StudentProgressionApi studentProgression;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transaction;

    public CourseCompletionService(CourseCompletionRepository completions, OutboxEventRepository outbox,
            StudentProgressionApi studentProgression, ApplicationEventPublisher eventPublisher,
            TransactionTemplate transaction) {
        this.completions = completions;
        this.outbox = outbox;
        this.studentProgression = studentProgression;
        this.eventPublisher = eventPublisher;
        this.transaction = transaction;
    }

    @Override
    public CourseCompletionDTO completeCourse(String courseId, Long studentId) {
        Course course = CourseCatalog.find(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));
        for (int attempt = 1; ; attempt++) {
            try {
                return transaction.execute(status -> complete(course, studentId));
            } catch (ConcurrencyFailureException | DataIntegrityViolationException conflict) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new StudentUpdateConflictException(studentId, conflict);
                }
                long millis = ThreadLocalRandom.current().nextLong(BACKOFF_STEP_MILLIS * attempt + 1);
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(millis));
            }
        }
    }

    private CourseCompletionDTO complete(Course course, Long studentId) {
        Optional<CourseCompletionEntity> existing = completions.findByStudentIdAndCourseId(studentId, course.courseId());
        if (existing.isPresent()) {
            StudentScoreDTO score = studentProgression.getScore(studentId);
            return new CourseCompletionDTO(existing.get().getId(), studentId, course.courseId(),
                    existing.get().getXpAwarded(), score.xpTotal(), score.level(), true);
        }
        Instant now = Instant.now();
        CourseCompletionEntity completion = completions.saveAndFlush(
                new CourseCompletionEntity(studentId, course.courseId(), course.xpReward(), now));
        StudentResponseDTO student = studentProgression.grantXpInCurrentTransaction(studentId,
                new ReceiveXpRequestDTO("CURSO_CONCLUIDO:" + course.courseId(), course.xpReward()));
        OutboxEventEntity event = outbox.save(
                new OutboxEventEntity(studentId, course.courseId(), course.xpReward(), now));
        eventPublisher.publishEvent(new OutboxEventCreated(event.getId()));
        return new CourseCompletionDTO(completion.getId(), studentId, course.courseId(), course.xpReward(),
                student.xpTotal(), student.level(), false);
    }

    @Override
    public List<CourseEventDTO> listEvents() {
        return outbox.findTop100ByOrderByIdDesc().stream()
                .map(event -> new CourseEventDTO(event.getId(), event.getStudentId(), event.getCourseId(),
                        event.getStatus().name(), event.getAttempts(), event.getLastError()))
                .toList();
    }

    @Override
    public void requeueEvent(Long eventId) {
        if (outbox.requeueDead(eventId, Instant.now()) == 0) {
            throw new CourseEventNotFoundException(eventId);
        }
    }
}

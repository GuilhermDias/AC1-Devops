package org.example.ac1devops.progression.internal;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.example.ac1devops.progression.api.CreateStudentRequestDTO;
import org.example.ac1devops.progression.api.Level;
import org.example.ac1devops.progression.api.LevelUpEventDTO;
import org.example.ac1devops.progression.api.ReceiveXpRequestDTO;
import org.example.ac1devops.progression.api.StudentNotFoundException;
import org.example.ac1devops.progression.api.StudentProgressionApi;
import org.example.ac1devops.progression.api.StudentResponseDTO;
import org.example.ac1devops.progression.api.StudentScoreChangedEvent;
import org.example.ac1devops.progression.api.StudentScoreDTO;
import org.example.ac1devops.progression.api.StudentUpdateConflictException;
import org.example.ac1devops.progression.internal.domain.LevelUpEvent;
import org.example.ac1devops.progression.internal.domain.Student;
import org.example.ac1devops.progression.internal.persistence.LevelUpEventEntity;
import org.example.ac1devops.progression.internal.persistence.StudentEntity;
import org.example.ac1devops.progression.internal.persistence.StudentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao interna de {@link StudentProgressionApi}: ponte entre {@link StudentEntity}
 * (persistencia) e {@link Student} (regra de negocio de XP/nivel, ja testada a 100% no dominio).
 * A entity guarda o estado final; quem recalcula transicoes de nivel continua sendo o dominio.
 *
 * <p>Publica {@link StudentScoreChangedEvent} quando um aluno e criado ou o XP total muda, para
 * que outros modulos (ex.: o cache do ranking) reajam sem dependencia de volta para este modulo.
 *
 * <p>Requisicoes simultaneas no mesmo aluno sao resolvidas por bloqueio otimista: quem perde a
 * disputa le o estado de novo e repete, um numero limitado de vezes. XP, nivel e level-ups sao
 * gravados juntos, e o evento so sai depois da gravacao que valeu, uma unica vez.
 */
@Service
public class StudentService implements StudentProgressionApi {

    static final int MAX_ATTEMPTS = 15;
    private static final long BACKOFF_STEP_MILLIS = 20;

    private final StudentRepository studentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StudentService(StudentRepository studentRepository, ApplicationEventPublisher eventPublisher) {
        this.studentRepository = studentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public StudentResponseDTO createStudent(CreateStudentRequestDTO request) {
        StudentEntity entity = studentRepository.save(new StudentEntity(request.name()));
        StudentResponseDTO response = toResponseDTO(entity);
        eventPublisher.publishEvent(new StudentScoreChangedEvent(entity.getId(), entity.getXpTotal()));
        return response;
    }

    @Override
    public StudentResponseDTO receiveXp(Long id, ReceiveXpRequestDTO request) {
        AppliedXp applied = applyXpWithRetry(id, request);
        // A resposta e montada antes do evento: um rollback em um listener limpa a sessao do
        // open-in-view, e a colecao lazy de level-ups nao poderia mais ser lida depois.
        StudentResponseDTO response = toResponseDTO(applied.student());
        if (applied.xpChanged()) {
            eventPublisher.publishEvent(
                    new StudentScoreChangedEvent(applied.student().getId(), applied.student().getXpTotal()));
        }
        return response;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public StudentResponseDTO grantXpInCurrentTransaction(Long id, ReceiveXpRequestDTO request) {
        return toResponseDTO(applyXp(id, request).student());
    }

    private AppliedXp applyXpWithRetry(Long id, ReceiveXpRequestDTO request) {
        for (int attempt = 1; ; attempt++) {
            try {
                return applyXp(id, request);
            } catch (ConcurrencyFailureException conflict) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new StudentUpdateConflictException(id, conflict);
                }
                waitBeforeRetry(attempt);
            }
        }
    }

    /**
     * Le o estado atual, aplica a regra de dominio e grava tudo de uma vez. O flush faz o conflito
     * aparecer aqui dentro: o rollback resultante limpa a sessao, entao a proxima tentativa le do banco.
     */
    private AppliedXp applyXp(Long id, ReceiveXpRequestDTO request) {
        StudentEntity entity = findEntityOrThrow(id);

        Student student = toDomain(entity);
        int eventsBefore = student.getLevelUpEvents().size();
        int xpBefore = student.getXpTotal();
        student.receiveXp(request.reason(), request.amount());

        entity.setXpTotal(student.getXpTotal());
        entity.setLevel(student.getLevel());
        List<LevelUpEvent> newEvents = student.getLevelUpEvents()
                .subList(eventsBefore, student.getLevelUpEvents().size());
        for (LevelUpEvent event : newEvents) {
            entity.addLevelUpEvent(new LevelUpEventEntity(event.getFromLevel(), event.getToLevel()));
        }

        return new AppliedXp(studentRepository.saveAndFlush(entity), student.getXpTotal() != xpBefore);
    }

    /** Espera aleatoria e crescente, para as requisicoes em disputa nao colidirem de novo juntas. */
    private static void waitBeforeRetry(int attempt) {
        long millis = ThreadLocalRandom.current().nextLong(BACKOFF_STEP_MILLIS * attempt + 1);
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(millis));
    }

    @Override
    public StudentResponseDTO getStudent(Long id) {
        return toResponseDTO(findEntityOrThrow(id));
    }

    @Override
    public List<StudentResponseDTO> listStudents() {
        return studentRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    @Override
    public Page<StudentScoreDTO> listByScore(Pageable pageable) {
        return studentRepository.findScoresOrderedByXp(
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
    }

    @Override
    public StudentScoreDTO getScore(Long id) {
        return studentRepository.findScoreById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }

    @Override
    public long countStudentsAhead(StudentScoreDTO score) {
        return studentRepository.countAhead(score.xpTotal(), score.id());
    }

    private StudentEntity findEntityOrThrow(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }

    /**
     * Reconstroi um {@link Student} de dominio a partir do XP total ja persistido. Como
     * {@link Level#fromXp} e {@code Student.processLevelUp} dependem so do total acumulado (nao
     * do caminho), um unico {@code receiveXp} com o total ja salvo recria o mesmo nivel e a
     * mesma sequencia de eventos que ja estao na entity.
     */
    private Student toDomain(StudentEntity entity) {
        Student student = new Student(entity.getName());
        if (entity.getXpTotal() > 0) {
            student.receiveXp("RECONSTRUCAO_ESTADO", entity.getXpTotal());
        }
        return student;
    }

    private StudentResponseDTO toResponseDTO(StudentEntity entity) {
        List<LevelUpEventDTO> events = entity.getLevelUpEvents().stream()
                .map(event -> new LevelUpEventDTO(event.getFromLevel(), event.getToLevel()))
                .toList();
        return new StudentResponseDTO(
                entity.getId(), entity.getName(), entity.getXpTotal(), entity.getLevel(), events);
    }

    private record AppliedXp(StudentEntity student, boolean xpChanged) {
    }
}

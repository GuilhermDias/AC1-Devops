package org.example.ac1devops.progression.internal;

import java.util.List;
import org.example.ac1devops.progression.api.CreateStudentRequestDTO;
import org.example.ac1devops.progression.api.Level;
import org.example.ac1devops.progression.api.LevelUpEventDTO;
import org.example.ac1devops.progression.api.ReceiveXpRequestDTO;
import org.example.ac1devops.progression.api.StudentNotFoundException;
import org.example.ac1devops.progression.api.StudentProgressionApi;
import org.example.ac1devops.progression.api.StudentResponseDTO;
import org.example.ac1devops.progression.api.StudentScoreChangedEvent;
import org.example.ac1devops.progression.api.StudentScoreDTO;
import org.example.ac1devops.progression.internal.domain.LevelUpEvent;
import org.example.ac1devops.progression.internal.domain.Student;
import org.example.ac1devops.progression.internal.persistence.LevelUpEventEntity;
import org.example.ac1devops.progression.internal.persistence.StudentEntity;
import org.example.ac1devops.progression.internal.persistence.StudentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Implementacao interna de {@link StudentProgressionApi}: ponte entre {@link StudentEntity}
 * (persistencia) e {@link Student} (regra de negocio de XP/nivel, ja testada a 100% no dominio).
 * A entity guarda o estado final; quem recalcula transicoes de nivel continua sendo o dominio.
 *
 * <p>Publica {@link StudentScoreChangedEvent} quando um aluno e criado ou o XP total muda, para
 * que outros modulos (ex.: o cache do ranking) reajam sem dependencia de volta para este modulo.
 */
@Service
public class StudentService implements StudentProgressionApi {

    private final StudentRepository studentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StudentService(StudentRepository studentRepository, ApplicationEventPublisher eventPublisher) {
        this.studentRepository = studentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public StudentResponseDTO createStudent(CreateStudentRequestDTO request) {
        StudentEntity entity = studentRepository.save(new StudentEntity(request.name()));
        eventPublisher.publishEvent(new StudentScoreChangedEvent(entity.getId(), entity.getXpTotal()));
        return toResponseDTO(entity);
    }

    @Override
    public StudentResponseDTO receiveXp(Long id, ReceiveXpRequestDTO request) {
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

        StudentEntity saved = studentRepository.save(entity);
        if (student.getXpTotal() != xpBefore) {
            eventPublisher.publishEvent(new StudentScoreChangedEvent(saved.getId(), saved.getXpTotal()));
        }
        return toResponseDTO(saved);
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
}

package org.example.ac1devops.service;

import java.util.List;
import org.example.ac1devops.domain.Level;
import org.example.ac1devops.domain.LevelUpEvent;
import org.example.ac1devops.domain.Student;
import org.example.ac1devops.dto.CreateStudentRequestDTO;
import org.example.ac1devops.dto.LevelUpEventDTO;
import org.example.ac1devops.dto.ReceiveXpRequestDTO;
import org.example.ac1devops.dto.StudentResponseDTO;
import org.example.ac1devops.entity.LevelUpEventEntity;
import org.example.ac1devops.entity.StudentEntity;
import org.example.ac1devops.repository.StudentRepository;
import org.springframework.stereotype.Service;

/**
 * Ponte entre {@link StudentEntity} (persistencia) e {@link Student} (regra de negocio de
 * XP/nivel, ja testada a 100% no dominio). A entity guarda o estado final; quem recalcula
 * transicoes de nivel continua sendo o dominio.
 */
@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public StudentResponseDTO createStudent(CreateStudentRequestDTO request) {
        StudentEntity entity = studentRepository.save(new StudentEntity(request.name()));
        return toResponseDTO(entity);
    }

    public StudentResponseDTO receiveXp(Long id, ReceiveXpRequestDTO request) {
        StudentEntity entity = findEntityOrThrow(id);

        Student student = toDomain(entity);
        int eventsBefore = student.getLevelUpEvents().size();
        student.receiveXp(request.reason(), request.amount());

        entity.setXpTotal(student.getXpTotal());
        entity.setLevel(student.getLevel());
        List<LevelUpEvent> newEvents = student.getLevelUpEvents()
                .subList(eventsBefore, student.getLevelUpEvents().size());
        for (LevelUpEvent event : newEvents) {
            entity.addLevelUpEvent(new LevelUpEventEntity(event.getFromLevel(), event.getToLevel()));
        }

        return toResponseDTO(studentRepository.save(entity));
    }

    public StudentResponseDTO getStudent(Long id) {
        return toResponseDTO(findEntityOrThrow(id));
    }

    public List<StudentResponseDTO> listStudents() {
        return studentRepository.findAll().stream().map(this::toResponseDTO).toList();
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

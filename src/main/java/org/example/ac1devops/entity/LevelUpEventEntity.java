package org.example.ac1devops.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.example.ac1devops.domain.Level;

/**
 * Representacao JPA de um {@link org.example.ac1devops.domain.LevelUpEvent} pertencente a um
 * {@link StudentEntity}.
 */
@Entity
public class LevelUpEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Level fromLevel;

    @Enumerated(EnumType.STRING)
    private Level toLevel;

    @ManyToOne
    private StudentEntity student;

    protected LevelUpEventEntity() {
    }

    public LevelUpEventEntity(Level fromLevel, Level toLevel) {
        this.fromLevel = fromLevel;
        this.toLevel = toLevel;
    }

    public Long getId() {
        return id;
    }

    public Level getFromLevel() {
        return fromLevel;
    }

    public Level getToLevel() {
        return toLevel;
    }

    public StudentEntity getStudent() {
        return student;
    }

    public void setStudent(StudentEntity student) {
        this.student = student;
    }
}

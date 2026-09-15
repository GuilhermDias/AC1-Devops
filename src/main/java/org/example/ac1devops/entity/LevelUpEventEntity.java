package org.example.ac1devops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.example.ac1devops.domain.Level;

/**
 * Representacao JPA de um {@link org.example.ac1devops.domain.LevelUpEvent} pertencente a um
 * {@link StudentEntity}.
 */
@Entity
@Table(name = "level_up_events")
public class LevelUpEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_level", nullable = false, length = 20)
    private Level fromLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_level", nullable = false, length = 20)
    private Level toLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
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

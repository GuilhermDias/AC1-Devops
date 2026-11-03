package org.example.ac1devops.cursos.internal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Conclusao de um curso. A restricao unica garante uma conclusao, e portanto uma recompensa, por aluno
 * e curso, mesmo com requisicoes simultaneas. {@code student_id} nao tem chave estrangeira porque
 * {@code students} pertence a outro modulo (ADR-001).
 */
@Entity
@Table(name = "course_completions", uniqueConstraints = @UniqueConstraint(
        name = "uk_course_completions_student_course", columnNames = {"student_id", "course_id"}))
public class CourseCompletionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_id", nullable = false, length = 50)
    private String courseId;

    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected CourseCompletionEntity() {
    }

    public CourseCompletionEntity(Long studentId, String courseId, int xpAwarded, Instant completedAt) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.xpAwarded = xpAwarded;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public int getXpAwarded() {
        return xpAwarded;
    }
}

package org.example.ac1devops.cursos.internal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Outbox transacional: o evento e gravado na mesma transacao da conclusao, entao nao ha conclusao sem
 * evento nem evento sem conclusao, mesmo que o processo caia logo depois do commit.
 */
@Entity
@Table(name = "course_outbox_events",
        indexes = @Index(name = "idx_course_outbox_due", columnList = "status, next_attempt_at"))
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_id", nullable = false, length = 50)
    private String courseId;

    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(Long studentId, String courseId, int xpAwarded, Instant nextAttemptAt) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.xpAwarded = xpAwarded;
        this.status = OutboxStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt;
    }

    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getCourseId() {
        return courseId;
    }

    public int getXpAwarded() {
        return xpAwarded;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }
}

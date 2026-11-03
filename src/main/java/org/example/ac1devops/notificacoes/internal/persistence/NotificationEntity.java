package org.example.ac1devops.notificacoes.internal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/** Notificacao enviada. A restricao unica em {@code event_id} torna o consumo idempotente. */
@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(
        name = "uk_notifications_event", columnNames = "event_id"))
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    protected NotificationEntity() {
    }

    public NotificationEntity(Long eventId, Long studentId, String message, Instant sentAt) {
        this.eventId = eventId;
        this.studentId = studentId;
        this.message = message;
        this.sentAt = sentAt;
    }

    public Long getId() {
        return id;
    }

    public Long getEventId() {
        return eventId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}

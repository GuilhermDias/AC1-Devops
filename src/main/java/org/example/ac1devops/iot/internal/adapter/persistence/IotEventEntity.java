package org.example.ac1devops.iot.internal.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.example.ac1devops.iot.internal.domain.DeviceEvent;
import org.example.ac1devops.iot.internal.domain.EventType;
import org.example.ac1devops.iot.internal.domain.ProcessingResult;
import org.example.ac1devops.iot.internal.domain.StoredDeviceEvent;

/** Evento recebido. A restricao unica em {@code event_id} descarta reentregas mesmo sob concorrencia. */
@Entity
@Table(name = "iot_events", uniqueConstraints = @UniqueConstraint(
        name = "uk_iot_events_event", columnNames = "event_id"))
public class IotEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private EventType type;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "activity_id", nullable = false, length = 64)
    private String activityId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessingResult status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "duplicates_discarded", nullable = false)
    private int duplicatesDiscarded;

    protected IotEventEntity() {
    }

    IotEventEntity(DeviceEvent event, Instant receivedAt, ProcessingResult status, String rejectionReason) {
        this.eventId = event.eventId();
        this.deviceId = event.deviceId();
        this.type = event.type();
        this.studentId = event.studentId();
        this.activityId = event.activityId();
        this.occurredAt = event.occurredAt();
        this.receivedAt = receivedAt;
        this.status = status;
        this.rejectionReason = rejectionReason;
    }

    StoredDeviceEvent toDomain() {
        return new StoredDeviceEvent(new DeviceEvent(eventId, deviceId, type, studentId, activityId, occurredAt),
                receivedAt, status, rejectionReason, duplicatesDiscarded);
    }
}

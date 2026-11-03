package org.example.ac1devops.iot.api;

import java.time.Instant;

public record DeviceEventDTO(
        String eventId,
        String deviceId,
        String type,
        Long studentId,
        String activityId,
        Instant occurredAt,
        Instant receivedAt,
        String status,
        String rejectionReason,
        int duplicatesDiscarded) {
}

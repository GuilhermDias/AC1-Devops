package org.example.ac1devops.iot.internal.domain;

import java.time.Instant;

public record StoredDeviceEvent(
        DeviceEvent event,
        Instant receivedAt,
        ProcessingResult status,
        String rejectionReason,
        int duplicatesDiscarded) {
}

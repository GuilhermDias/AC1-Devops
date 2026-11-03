package org.example.ac1devops.iot.internal.domain;

import java.util.List;

/** Porta do registro de eventos ja processados, que garante a idempotencia por {@code eventId}. */
public interface DeviceEventStore {

    boolean isKnown(String eventId);

    /** @return {@code false} se outra entrega do mesmo evento ja tiver sido registrada */
    boolean save(DeviceEvent event, ProcessingResult status, String rejectionReason);

    void countDuplicate(String eventId);

    List<StoredDeviceEvent> recent();
}

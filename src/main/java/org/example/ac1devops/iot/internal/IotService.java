package org.example.ac1devops.iot.internal;

import java.util.List;
import org.example.ac1devops.iot.api.DeviceEventDTO;
import org.example.ac1devops.iot.api.IotApi;
import org.example.ac1devops.iot.internal.domain.DeviceEventStore;
import org.springframework.stereotype.Service;

/** Implementacao de {@link IotApi}. */
@Service
public class IotService implements IotApi {

    private final DeviceEventStore store;

    public IotService(DeviceEventStore store) {
        this.store = store;
    }

    @Override
    public List<DeviceEventDTO> listEvents() {
        return store.recent().stream()
                .map(stored -> new DeviceEventDTO(stored.event().eventId(), stored.event().deviceId(),
                        stored.event().type().name(), stored.event().studentId(), stored.event().activityId(),
                        stored.event().occurredAt(), stored.receivedAt(), stored.status().name(),
                        stored.rejectionReason(), stored.duplicatesDiscarded()))
                .toList();
    }
}

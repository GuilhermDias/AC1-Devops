package org.example.ac1devops.iot.internal.adapter.persistence;

import java.time.Instant;
import java.util.List;
import org.example.ac1devops.iot.internal.domain.DeviceEvent;
import org.example.ac1devops.iot.internal.domain.DeviceEventStore;
import org.example.ac1devops.iot.internal.domain.ProcessingResult;
import org.example.ac1devops.iot.internal.domain.StoredDeviceEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class JpaDeviceEventStore implements DeviceEventStore {

    private final IotEventRepository repository;

    public JpaDeviceEventStore(IotEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isKnown(String eventId) {
        return repository.existsByEventId(eventId);
    }

    @Override
    public boolean save(DeviceEvent event, ProcessingResult status, String rejectionReason) {
        try {
            repository.saveAndFlush(new IotEventEntity(event, Instant.now(), status, rejectionReason));
            return true;
        } catch (DataIntegrityViolationException alreadyRegistered) {
            return false;
        }
    }

    @Override
    public void countDuplicate(String eventId) {
        repository.countDuplicate(eventId);
    }

    @Override
    public List<StoredDeviceEvent> recent() {
        return repository.findTop100ByOrderByIdDesc().stream().map(IotEventEntity::toDomain).toList();
    }
}

package org.example.ac1devops.iot.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.example.ac1devops.iot.api.DeviceEventDTO;
import org.example.ac1devops.iot.internal.domain.DeviceEvent;
import org.example.ac1devops.iot.internal.domain.DeviceEventStore;
import org.example.ac1devops.iot.internal.domain.EventType;
import org.example.ac1devops.iot.internal.domain.ProcessingResult;
import org.example.ac1devops.iot.internal.domain.StoredDeviceEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IotServiceTest {

    @Mock
    private DeviceEventStore store;

    @Test
    void deveTraduzirOsEventosParaOContratoPublico() {
        Instant now = Instant.parse("2026-11-03T12:00:00Z");
        DeviceEvent event = new DeviceEvent("e1", "sensor-01", EventType.CONCLUSAO, 7L, "x", now);
        when(store.recent()).thenReturn(List.of(new StoredDeviceEvent(event, now, ProcessingResult.REJECTED, "motivo", 1)));

        assertThat(new IotService(store).listEvents()).containsExactly(new DeviceEventDTO(
                "e1", "sensor-01", "CONCLUSAO", 7L, "x", now, now, "REJECTED", "motivo", 1));
    }
}

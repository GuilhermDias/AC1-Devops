package org.example.ac1devops.iot.internal.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.example.ac1devops.iot.internal.domain.DeviceEvent;
import org.example.ac1devops.iot.internal.domain.EventType;
import org.example.ac1devops.iot.internal.domain.ProcessingResult;
import org.example.ac1devops.iot.internal.domain.StoredDeviceEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Sem a transacao do teste, como no gateway: cada chamada grava de verdade. */
@DataJpaTest
@Import(JpaDeviceEventStore.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaDeviceEventStoreTest {

    @Autowired
    private JpaDeviceEventStore store;

    @Test
    void deveDescartarSegundoRegistroDoMesmoEventoEContarADuplicata() {
        DeviceEvent event = new DeviceEvent(
                "store-1", "lab-01", EventType.INICIO, 7L, "ci-cd", Instant.parse("2026-11-03T12:00:00Z"));

        assertThat(store.isKnown("store-1")).isFalse();
        assertThat(store.save(event, ProcessingResult.ACCEPTED, null)).isTrue();
        assertThat(store.save(event, ProcessingResult.ACCEPTED, null)).isFalse();
        store.countDuplicate("store-1");

        assertThat(store.isKnown("store-1")).isTrue();
        StoredDeviceEvent stored = store.recent().get(0);
        assertThat(stored.event()).isEqualTo(event);
        assertThat(stored.status()).isEqualTo(ProcessingResult.ACCEPTED);
        assertThat(stored.duplicatesDiscarded()).isEqualTo(1);
    }
}

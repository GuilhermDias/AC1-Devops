package org.example.ac1devops.iot.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeviceEventTest {

    private static final String TIMESTAMP = "2026-11-03T12:00:00Z";

    @Test
    void deveTransformarOsCamposDoDispositivoParaOModeloDaAplicacao() {
        DeviceEvent event = DeviceEvent.of("e1", "lab-01", "conclusao", 7L, "ci-cd", TIMESTAMP);

        assertThat(event).isEqualTo(new DeviceEvent(
                "e1", "lab-01", EventType.CONCLUSAO, 7L, "ci-cd", Instant.parse(TIMESTAMP)));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULO", value = {
        "NULO, lab-01, presenca, 7, ci-cd, 2026-11-03T12:00:00Z",
        "'  ', lab-01, presenca, 7, ci-cd, 2026-11-03T12:00:00Z",
        "e1, NULO, presenca, 7, ci-cd, 2026-11-03T12:00:00Z",
        "e1, lab-01, desconhecido, 7, ci-cd, 2026-11-03T12:00:00Z",
        "e1, lab-01, NULO, 7, ci-cd, 2026-11-03T12:00:00Z",
        "e1, lab-01, presenca, NULO, ci-cd, 2026-11-03T12:00:00Z",
        "e1, lab-01, presenca, 0, ci-cd, 2026-11-03T12:00:00Z",
        "e1, lab-01, presenca, 7, NULO, 2026-11-03T12:00:00Z",
        "e1, lab-01, presenca, 7, ci-cd, ontem",
        "e1, lab-01, presenca, 7, ci-cd, NULO"
    })
    void deveRejeitarEventoInvalido(String eventId, String deviceId, String type, Long studentId, String activityId,
            String timestamp) {
        assertThatThrownBy(() -> DeviceEvent.of(eventId, deviceId, type, studentId, activityId, timestamp))
                .isInstanceOf(InvalidDeviceEventException.class);
    }

    @Test
    void deveRejeitarTextoMaiorQueAColunaDoBanco() {
        assertThatThrownBy(() -> DeviceEvent.of("e".repeat(65), "lab-01", "presenca", 7L, "ci-cd", TIMESTAMP))
                .isInstanceOf(InvalidDeviceEventException.class);
    }
}

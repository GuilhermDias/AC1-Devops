package org.example.ac1devops.iot.internal.adapter.mqtt;

import org.example.ac1devops.iot.internal.domain.DeviceEvent;
import org.example.ac1devops.iot.internal.domain.InvalidDeviceEventException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Traduz a mensagem MQTT para o dominio e confere se o dispositivo publicou no proprio topico. */
final class DeviceEventParser {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int DEVICE_SEGMENT = 3;

    private DeviceEventParser() {
    }

    static DeviceEvent parse(String topic, byte[] body) {
        Payload payload = read(body);
        DeviceEvent event = DeviceEvent.of(payload.eventId(), payload.deviceId(), payload.tipo(), payload.alunoId(),
                payload.atividadeId(), payload.timestamp());
        String topicDevice = topic.split("/")[DEVICE_SEGMENT];
        if (!topicDevice.equals(event.deviceId())) {
            throw new InvalidDeviceEventException(
                    "deviceId " + event.deviceId() + " diferente do topico " + topicDevice);
        }
        return event;
    }

    private static Payload read(byte[] body) {
        try {
            Payload payload = JSON.readValue(body, Payload.class);
            if (payload == null) {
                throw new InvalidDeviceEventException("corpo vazio");
            }
            return payload;
        } catch (JacksonException malformed) {
            throw new InvalidDeviceEventException("JSON invalido");
        }
    }

    private record Payload(
            String eventId, String deviceId, String tipo, Long alunoId, String atividadeId, String timestamp) {
    }
}

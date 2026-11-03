package org.example.ac1devops.iot.internal.domain;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/** Evento de um dispositivo ja validado e no modelo da aplicacao. */
public record DeviceEvent(
        String eventId,
        String deviceId,
        EventType type,
        Long studentId,
        String activityId,
        Instant occurredAt) {

    private static final int MAX_TEXT_LENGTH = 64;

    /**
     * Valida e transforma os campos recebidos do dispositivo.
     *
     * @throws InvalidDeviceEventException se algum campo estiver ausente ou invalido
     */
    public static DeviceEvent of(String eventId, String deviceId, String type, Long studentId, String activityId,
            String timestamp) {
        if (studentId == null || studentId <= 0) {
            throw new InvalidDeviceEventException("alunoId invalido: " + studentId);
        }
        return new DeviceEvent(
                requireText("eventId", eventId),
                requireText("deviceId", deviceId),
                EventType.fromWire(type).orElseThrow(() -> new InvalidDeviceEventException("tipo invalido: " + type)),
                studentId,
                requireText("atividadeId", activityId),
                parseInstant(timestamp));
    }

    private static String requireText(String name, String value) {
        if (value == null || value.isBlank() || value.length() > MAX_TEXT_LENGTH) {
            throw new InvalidDeviceEventException(name + " deve ter de 1 a " + MAX_TEXT_LENGTH + " caracteres");
        }
        return value;
    }

    private static Instant parseInstant(String timestamp) {
        try {
            return Instant.parse(String.valueOf(timestamp));
        } catch (DateTimeParseException invalid) {
            throw new InvalidDeviceEventException("timestamp invalido: " + timestamp);
        }
    }
}

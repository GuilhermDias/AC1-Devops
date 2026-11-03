package org.example.ac1devops.iot.internal.domain;

import java.util.Arrays;
import java.util.Optional;

public enum EventType {
    PRESENCA("presenca"),
    INICIO("inicio"),
    CONCLUSAO("conclusao");

    private final String wireValue;

    EventType(String wireValue) {
        this.wireValue = wireValue;
    }

    static Optional<EventType> fromWire(String value) {
        return Arrays.stream(values()).filter(type -> type.wireValue.equals(value)).findFirst();
    }
}

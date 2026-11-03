package org.example.ac1devops.iot.internal.domain;

/** Evento que nunca sera valido: nao adianta entregar de novo. */
public class InvalidDeviceEventException extends RuntimeException {

    public InvalidDeviceEventException(String message) {
        super(message);
    }
}

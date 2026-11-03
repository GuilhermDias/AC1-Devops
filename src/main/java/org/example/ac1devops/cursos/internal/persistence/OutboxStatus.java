package org.example.ac1devops.cursos.internal.persistence;

/** {@code DEAD}: esgotou as tentativas e so volta para a fila por reenvio manual. */
public enum OutboxStatus {
    PENDING,
    DELIVERED,
    DEAD
}

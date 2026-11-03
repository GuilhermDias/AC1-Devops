package org.example.ac1devops.cursos.internal;

/** Aviso interno de que um evento entrou na outbox, para a primeira entrega nao esperar a varredura. */
record OutboxEventCreated(Long eventId) {
}

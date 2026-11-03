package org.example.ac1devops.notificacoes.api;

import java.time.Instant;

public record NotificationDTO(Long id, Long eventId, Long studentId, String message, Instant sentAt) {
}

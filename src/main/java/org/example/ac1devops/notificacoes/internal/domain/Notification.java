package org.example.ac1devops.notificacoes.internal.domain;

public record Notification(Long eventId, Long studentId, String message) {

    public static Notification courseCompleted(Long eventId, Long studentId, String courseId, int xpAwarded) {
        return new Notification(eventId, studentId,
                "Parabens! Voce concluiu o curso " + courseId + " e ganhou " + xpAwarded + " XP.");
    }
}

package org.example.ac1devops.notificacoes.internal;

import java.time.Instant;
import java.util.List;
import org.example.ac1devops.cursos.api.CursoConcluidoEvent;
import org.example.ac1devops.notificacoes.api.NotificacoesApi;
import org.example.ac1devops.notificacoes.api.NotificationDTO;
import org.example.ac1devops.notificacoes.internal.domain.Notification;
import org.example.ac1devops.notificacoes.internal.domain.NotificationChannel;
import org.example.ac1devops.notificacoes.internal.persistence.NotificationEntity;
import org.example.ac1devops.notificacoes.internal.persistence.NotificationRepository;
import org.example.ac1devops.progression.api.StudentProgressionApi;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Consumidor de {@link CursoConcluidoEvent}. Ignora eventos repetidos e deixa a excecao do canal subir
 * para quem entrega o evento, que agenda outra tentativa. Uma queda entre o envio e a gravacao pode
 * repetir um envio: a entrega e ao menos uma vez.
 */
@Service
public class NotificacaoService implements NotificacoesApi {

    private final NotificationRepository notifications;
    private final NotificationChannel channel;
    private final StudentProgressionApi studentProgression;

    public NotificacaoService(NotificationRepository notifications, NotificationChannel channel,
            StudentProgressionApi studentProgression) {
        this.notifications = notifications;
        this.channel = channel;
        this.studentProgression = studentProgression;
    }

    @EventListener
    public void onCourseCompleted(CursoConcluidoEvent event) {
        if (notifications.existsByEventId(event.eventId())) {
            return;
        }
        Notification notification = Notification.courseCompleted(
                event.eventId(), event.studentId(), event.courseId(), event.xpAwarded());
        channel.send(notification);
        notifications.save(new NotificationEntity(
                notification.eventId(), notification.studentId(), notification.message(), Instant.now()));
    }

    @Override
    public List<NotificationDTO> listByStudent(Long studentId) {
        studentProgression.getScore(studentId);
        return notifications.findByStudentIdOrderById(studentId).stream()
                .map(entity -> new NotificationDTO(entity.getId(), entity.getEventId(), entity.getStudentId(),
                        entity.getMessage(), entity.getSentAt()))
                .toList();
    }
}

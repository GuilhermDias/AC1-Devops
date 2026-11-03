package org.example.ac1devops.notificacoes.internal.adapter.channel;

import org.example.ac1devops.notificacoes.internal.domain.Notification;
import org.example.ac1devops.notificacoes.internal.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Canal simulado, que so registra no log. {@code NOTIFICACOES_CANAL_DISPONIVEL=false} simula o
 * provedor externo fora do ar, para o roteiro de evidencia.
 */
@Component
public class SimulatedNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SimulatedNotificationChannel.class);

    private final boolean available;

    public SimulatedNotificationChannel(@Value("${notificacoes.canal.disponivel:true}") boolean available) {
        this.available = available;
    }

    @Override
    public void send(Notification notification) {
        if (!available) {
            throw new IllegalStateException("Canal de notificacao indisponivel");
        }
        log.info("Notificacao enviada ao aluno {}: {}", notification.studentId(), notification.message());
    }
}

package org.example.ac1devops.cursos.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.example.ac1devops.cursos.api.CursoConcluidoEvent;
import org.example.ac1devops.cursos.internal.persistence.OutboxEventEntity;
import org.example.ac1devops.cursos.internal.persistence.OutboxEventRepository;
import org.example.ac1devops.cursos.internal.persistence.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Entrega os eventos da outbox fora da requisicao. A primeira tentativa sai logo depois do commit; a
 * varredura periodica cuida das novas tentativas e do que ficou pendente se o processo caiu. Os
 * consumidores rodam nesta thread, entao uma excecao deles chega aqui e agenda outra tentativa.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int SWEEP_BATCH_SIZE = 50;
    private static final int MAX_ERROR_LENGTH = 255;

    private final OutboxEventRepository outbox;
    private final ApplicationEventPublisher eventPublisher;
    private final OutboxProperties properties;

    public OutboxRelay(OutboxEventRepository outbox, ApplicationEventPublisher eventPublisher,
            OutboxProperties properties) {
        this.outbox = outbox;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Async
    @TransactionalEventListener
    public void onEventCreated(OutboxEventCreated created) {
        tryDeliver(created.eventId());
    }

    @Scheduled(fixedDelayString = "${cursos.outbox.sweep-interval-ms:5000}")
    public void sweep() {
        List<Long> dueIds = outbox.findDueIds(Instant.now(), PageRequest.of(0, SWEEP_BATCH_SIZE));
        dueIds.forEach(this::tryDeliver);
    }

    private void tryDeliver(Long eventId) {
        Instant now = Instant.now();
        if (outbox.claim(eventId, now, now.plus(properties.lease())) == 0) {
            return;
        }
        OutboxEventEntity event = outbox.findById(eventId).orElseThrow();
        try {
            eventPublisher.publishEvent(new CursoConcluidoEvent(
                    event.getId(), event.getStudentId(), event.getCourseId(), event.getXpAwarded()));
            outbox.markDelivered(eventId);
        } catch (RuntimeException failure) {
            recordFailure(event, failure);
        }
    }

    private void recordFailure(OutboxEventEntity event, RuntimeException failure) {
        int attempts = event.getAttempts();
        boolean exhausted = attempts >= properties.maxAttempts();
        Duration wait = properties.baseBackoff().multipliedBy(1L << (attempts - 1));
        String message = String.valueOf(failure.getMessage());
        outbox.markFailed(event.getId(), exhausted ? OutboxStatus.DEAD : OutboxStatus.PENDING, Instant.now().plus(wait),
                message.substring(0, Math.min(message.length(), MAX_ERROR_LENGTH)));
        log.warn("Falha ao entregar o evento {} (tentativa {} de {}, esgotou: {}): {}",
                event.getId(), attempts, properties.maxAttempts(), exhausted, message);
    }
}

package org.example.ac1devops.cursos.internal;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.example.ac1devops.cursos.internal.persistence.OutboxEventRepository;
import org.example.ac1devops.cursos.internal.persistence.OutboxStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Publica {@code outbox_events{status}}, que mostra se a entrega esta acompanhando as conclusoes (ASR11). */
@Component
public class OutboxMetrics {

    private final OutboxEventRepository outbox;
    private final Map<OutboxStatus, AtomicLong> counts = new EnumMap<>(OutboxStatus.class);

    public OutboxMetrics(OutboxEventRepository outbox) {
        this.outbox = outbox;
        for (OutboxStatus status : OutboxStatus.values()) {
            AtomicLong count = new AtomicLong();
            counts.put(status, count);
            Gauge.builder("outbox.events", count, AtomicLong::get)
                    .tag("status", status.name())
                    .register(Metrics.globalRegistry);
        }
    }

    @Scheduled(fixedDelayString = "${cursos.outbox.metrics-interval-ms:5000}")
    public void refresh() {
        counts.values().forEach(count -> count.set(0));
        outbox.countByStatus().forEach(row -> counts.get(row.getStatus()).set(row.getTotal()));
    }
}

package org.example.ac1devops.conquistas.internal.adapter.event;

import io.micrometer.core.instrument.Metrics;
import org.example.ac1devops.conquistas.internal.domain.BadgeAwardService;
import org.example.ac1devops.conquistas.internal.domain.StudentProgress;
import org.example.ac1devops.progression.api.Level;
import org.example.ac1devops.progression.api.StudentScoreChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Adapter de entrada: traduz o evento de progressao para o dominio. O evento roda na mesma thread
 * de {@code POST /students/{id}/xp}, e o XP ja foi salvo nesse ponto; uma falha aqui so e registrada
 * para nao devolver erro a quem recebeu o XP. Como a avaliacao e idempotente, o proximo evento do
 * aluno concede o que ficou faltando.
 */
@Component
public class StudentScoreEventListener {

    private static final Logger log = LoggerFactory.getLogger(StudentScoreEventListener.class);

    private final BadgeAwardService badgeAwardService;

    public StudentScoreEventListener(BadgeAwardService badgeAwardService) {
        this.badgeAwardService = badgeAwardService;
    }

    @EventListener
    public void onScoreChanged(StudentScoreChangedEvent event) {
        StudentProgress progress = new StudentProgress(
                event.studentId(), event.xpTotal(), Level.fromXp(event.xpTotal()));
        try {
            Metrics.counter("conquistas.badges.awarded").increment(badgeAwardService.evaluate(progress).size());
        } catch (RuntimeException exception) {
            log.warn("Falha ao avaliar conquistas do aluno {}", event.studentId(), exception);
        }
    }
}

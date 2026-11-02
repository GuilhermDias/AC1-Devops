package org.example.ac1devops.conquistas.internal.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regra de concessao de badges, sem Spring nem JPA: avalia as regras vigentes contra o progresso
 * do aluno e registra so o que ele ainda nao tem. E idempotente, e um badge concedido nunca e
 * retirado, mesmo que a regra deixe de existir.
 */
public class BadgeAwardService {

    private final AwardedBadgePort awardedBadges;
    private final List<BadgeRule> rules;

    public BadgeAwardService(AwardedBadgePort awardedBadges, List<BadgeRule> rules) {
        this.awardedBadges = awardedBadges;
        this.rules = List.copyOf(rules);
    }

    /** Concede os badges devidos e devolve apenas os novos. */
    public List<Badge> evaluate(StudentProgress progress) {
        Set<String> alreadyAwarded = awardedBadges.findByStudent(progress.studentId()).stream()
                .map(Badge::code)
                .collect(Collectors.toSet());
        List<Badge> newBadges = rules.stream()
                .filter(rule -> rule.isMetBy(progress))
                .map(BadgeRule::badge)
                .filter(badge -> !alreadyAwarded.contains(badge.code()))
                .toList();
        newBadges.forEach(badge -> awardedBadges.award(progress.studentId(), badge));
        return newBadges;
    }

    public List<Badge> badgesOf(Long studentId) {
        return awardedBadges.findByStudent(studentId);
    }
}

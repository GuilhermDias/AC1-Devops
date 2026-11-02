package org.example.ac1devops.conquistas.internal.domain;

import java.util.function.Predicate;

/** Regra de gamificacao: concede {@code badge} quando {@code condition} e verdadeira. */
public record BadgeRule(Badge badge, Predicate<StudentProgress> condition) {

    public boolean isMetBy(StudentProgress progress) {
        return condition.test(progress);
    }
}

package org.example.ac1devops.conquistas.internal.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Adapter em memoria da {@link AwardedBadgePort}: mesmo contrato do JPA, sem banco e sem Spring. */
class InMemoryAwardedBadgeAdapter implements AwardedBadgePort {

    private final Map<Long, List<Badge>> badgesByStudent = new HashMap<>();

    @Override
    public List<Badge> findByStudent(Long studentId) {
        return List.copyOf(badgesByStudent.getOrDefault(studentId, List.of()));
    }

    @Override
    public void award(Long studentId, Badge badge) {
        List<Badge> badges = badgesByStudent.computeIfAbsent(studentId, id -> new ArrayList<>());
        if (badges.stream().anyMatch(existing -> existing.code().equals(badge.code()))) {
            throw new IllegalStateException("Badge duplicado: " + badge.code());
        }
        badges.add(badge);
    }
}

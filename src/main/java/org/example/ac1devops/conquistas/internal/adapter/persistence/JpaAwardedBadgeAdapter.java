package org.example.ac1devops.conquistas.internal.adapter.persistence;

import java.util.List;
import org.example.ac1devops.conquistas.internal.domain.AwardedBadgePort;
import org.example.ac1devops.conquistas.internal.domain.Badge;
import org.springframework.stereotype.Component;

/** Adapter de producao da {@link AwardedBadgePort}: traduz entre o dominio e a tabela {@code student_badges}. */
@Component
public class JpaAwardedBadgeAdapter implements AwardedBadgePort {

    private final AwardedBadgeRepository repository;

    public JpaAwardedBadgeAdapter(AwardedBadgeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Badge> findByStudent(Long studentId) {
        return repository.findByStudentIdOrderById(studentId).stream()
                .map(entity -> new Badge(entity.getCode(), entity.getDescription()))
                .toList();
    }

    @Override
    public void award(Long studentId, Badge badge) {
        repository.save(new AwardedBadgeEntity(studentId, badge.code(), badge.description()));
    }
}

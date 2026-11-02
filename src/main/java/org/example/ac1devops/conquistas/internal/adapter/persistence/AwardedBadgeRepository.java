package org.example.ac1devops.conquistas.internal.adapter.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AwardedBadgeRepository extends JpaRepository<AwardedBadgeEntity, Long> {

    List<AwardedBadgeEntity> findByStudentIdOrderById(Long studentId);
}

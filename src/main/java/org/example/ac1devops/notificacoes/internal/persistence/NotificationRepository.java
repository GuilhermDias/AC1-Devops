package org.example.ac1devops.notificacoes.internal.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    boolean existsByEventId(Long eventId);

    List<NotificationEntity> findByStudentIdOrderById(Long studentId);
}

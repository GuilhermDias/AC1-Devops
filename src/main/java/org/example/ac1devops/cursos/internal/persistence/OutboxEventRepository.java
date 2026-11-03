package org.example.ac1devops.cursos.internal.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findTop100ByOrderByIdDesc();

    @Query("select e.id from OutboxEventEntity e "
            + "where e.status = org.example.ac1devops.cursos.internal.persistence.OutboxStatus.PENDING "
            + "and e.nextAttemptAt <= :now order by e.id")
    List<Long> findDueIds(@Param("now") Instant now, Pageable pageable);

    /**
     * Reserva o evento para uma tentativa. O update condicional e atomico: entre varias threads ou
     * replicas, so uma recebe 1. Se o processo cair, a reserva expira em {@code leaseUntil}.
     */
    @Transactional
    @Modifying
    @Query("update OutboxEventEntity e set e.attempts = e.attempts + 1, e.nextAttemptAt = :leaseUntil "
            + "where e.id = :id and e.nextAttemptAt <= :now "
            + "and e.status = org.example.ac1devops.cursos.internal.persistence.OutboxStatus.PENDING")
    int claim(@Param("id") Long id, @Param("now") Instant now, @Param("leaseUntil") Instant leaseUntil);

    @Transactional
    @Modifying
    @Query("update OutboxEventEntity e set e.lastError = null, "
            + "e.status = org.example.ac1devops.cursos.internal.persistence.OutboxStatus.DELIVERED "
            + "where e.id = :id")
    int markDelivered(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("update OutboxEventEntity e set e.status = :status, e.nextAttemptAt = :nextAttemptAt, "
            + "e.lastError = :lastError where e.id = :id")
    int markFailed(@Param("id") Long id, @Param("status") OutboxStatus status,
            @Param("nextAttemptAt") Instant nextAttemptAt, @Param("lastError") String lastError);

    @Transactional
    @Modifying
    @Query("update OutboxEventEntity e set e.attempts = 0, e.nextAttemptAt = :now, "
            + "e.status = org.example.ac1devops.cursos.internal.persistence.OutboxStatus.PENDING "
            + "where e.id = :id "
            + "and e.status = org.example.ac1devops.cursos.internal.persistence.OutboxStatus.DEAD")
    int requeueDead(@Param("id") Long id, @Param("now") Instant now);
}

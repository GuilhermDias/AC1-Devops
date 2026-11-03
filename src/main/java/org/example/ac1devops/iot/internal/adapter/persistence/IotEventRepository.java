package org.example.ac1devops.iot.internal.adapter.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface IotEventRepository extends JpaRepository<IotEventEntity, Long> {

    boolean existsByEventId(String eventId);

    List<IotEventEntity> findTop100ByOrderByIdDesc();

    @Transactional
    @Modifying
    @Query("update IotEventEntity e set e.duplicatesDiscarded = e.duplicatesDiscarded + 1 where e.eventId = :eventId")
    int countDuplicate(@Param("eventId") String eventId);
}

package com.hestia.event.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link CalendarEventEntity}. */
public interface CalendarEventJpaRepository extends JpaRepository<CalendarEventEntity, UUID> {

    List<CalendarEventEntity> findByHouseholdId(UUID householdId);
}

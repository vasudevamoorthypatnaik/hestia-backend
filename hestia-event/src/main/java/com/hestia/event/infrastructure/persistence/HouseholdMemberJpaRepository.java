package com.hestia.event.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link HouseholdMemberEntity}. */
public interface HouseholdMemberJpaRepository
        extends JpaRepository<HouseholdMemberEntity, UUID> {

    List<HouseholdMemberEntity> findByHouseholdId(UUID householdId);
}

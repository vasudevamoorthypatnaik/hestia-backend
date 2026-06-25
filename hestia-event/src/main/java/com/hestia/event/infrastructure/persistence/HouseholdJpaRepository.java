package com.hestia.event.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link HouseholdEntity}. */
public interface HouseholdJpaRepository extends JpaRepository<HouseholdEntity, UUID> {

    /** The default (oldest) household, selected at the DB with ORDER BY created_at LIMIT 1. */
    Optional<HouseholdEntity> findTop1ByOrderByCreatedAtAsc();
}

package com.hestia.event.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link HouseholdEntity}. */
public interface HouseholdJpaRepository extends JpaRepository<HouseholdEntity, UUID> {}

package com.hestia.event.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ConnectedAccountEntity}. */
public interface ConnectedAccountJpaRepository
        extends JpaRepository<ConnectedAccountEntity, UUID> {

    List<ConnectedAccountEntity> findByHouseholdId(UUID householdId);
}

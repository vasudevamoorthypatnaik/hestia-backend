package com.hestia.event.infrastructure.persistence;

import com.hestia.event.domain.SyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** JPA entity for the {@code connected_account} table (V002). */
@Entity
@Table(name = "connected_account")
public class ConnectedAccountEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncStatus status;

    protected ConnectedAccountEntity() {}

    public UUID getId() {
        return id;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public String getProvider() {
        return provider;
    }

    public String getLabel() {
        return label;
    }

    public SyncStatus getStatus() {
        return status;
    }
}

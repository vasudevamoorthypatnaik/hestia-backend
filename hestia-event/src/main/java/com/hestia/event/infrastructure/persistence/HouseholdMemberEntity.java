package com.hestia.event.infrastructure.persistence;

import com.hestia.event.domain.MemberKind;
import com.hestia.event.domain.MemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** JPA entity for the {@code household_member} table (V002). */
@Entity
@Table(name = "household_member")
public class HouseholdMemberEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String initial;

    @Column(name = "color_hex", nullable = false)
    private String colorHex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Column(name = "responsible_capable", nullable = false)
    private boolean responsibleCapable;

    @Column(name = "age_label")
    private String ageLabel;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected HouseholdMemberEntity() {}

    public UUID getId() {
        return id;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getInitial() {
        return initial;
    }

    public String getColorHex() {
        return colorHex;
    }

    public MemberKind getKind() {
        return kind;
    }

    public MemberRole getRole() {
        return role;
    }

    public boolean isResponsibleCapable() {
        return responsibleCapable;
    }

    public String getAgeLabel() {
        return ageLabel;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}

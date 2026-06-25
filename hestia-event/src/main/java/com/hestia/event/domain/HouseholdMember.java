package com.hestia.event.domain;

import java.util.UUID;

/**
 * A member of a household. Adults can be a "responsible adult"; children are schedule subjects
 * (owners) only. {@code colorHex} is the authoritative per-person color (rendered by the frontend).
 */
public record HouseholdMember(
        UUID id,
        String displayName,
        String initial,
        String colorHex,
        MemberKind kind,
        MemberRole role,
        boolean responsibleCapable,
        String ageLabel,
        int sortOrder) {}

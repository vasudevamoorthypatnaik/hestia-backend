package com.hestia.event.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Validated-at-the-service input for creating a calendar event. {@code startTime}/{@code endTime}
 * are "HH:mm" (24h) or null when {@code allDay}. {@code responsibleMemberId} null => Unassigned.
 */
public record CreateEventCommand(
        String title,
        LocalDate date,
        String startTime,
        String endTime,
        boolean allDay,
        List<UUID> ownerMemberIds,
        UUID responsibleMemberId,
        boolean needsDriver,
        String location) {}

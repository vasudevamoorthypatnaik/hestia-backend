package com.hestia.event.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A stored calendar event. Two shapes:
 *  - recurring seed event: {@code recurring=true}, {@code dayOfWeek} set (1=Mon..7=Sun),
 *    {@code eventDate} null — the service projects it onto whatever week/day is requested
 *    (so the seeded household always looks "current").
 *  - concrete event (e.g. user-created): {@code recurring=false}, {@code eventDate} set,
 *    {@code dayOfWeek} null.
 * Times are minutes-from-midnight in the household timezone; null when {@code allDay}.
 */
public record CalendarEventData(
        UUID id,
        UUID householdId,
        String title,
        boolean recurring,
        Integer dayOfWeek,
        LocalDate eventDate,
        Integer startMinute,
        Integer endMinute,
        boolean allDay,
        List<UUID> ownerMemberIds,
        UUID responsibleMemberId,
        String location,
        boolean needsDriver) {}

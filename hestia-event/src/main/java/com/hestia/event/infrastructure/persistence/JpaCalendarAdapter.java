package com.hestia.event.infrastructure.persistence;

import com.hestia.event.application.CalendarEventRepository;
import com.hestia.event.application.HouseholdRepository;
import com.hestia.event.domain.CalendarEventData;
import com.hestia.event.domain.ConnectedAccount;
import com.hestia.event.domain.Household;
import com.hestia.event.domain.HouseholdMember;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** JPA adapter implementing the calendar read/write ports (hexagonal infrastructure). */
@Repository
public class JpaCalendarAdapter implements HouseholdRepository, CalendarEventRepository {

    private final HouseholdJpaRepository households;
    private final HouseholdMemberJpaRepository members;
    private final ConnectedAccountJpaRepository accounts;
    private final CalendarEventJpaRepository events;

    public JpaCalendarAdapter(
            HouseholdJpaRepository households,
            HouseholdMemberJpaRepository members,
            ConnectedAccountJpaRepository accounts,
            CalendarEventJpaRepository events) {
        this.households = households;
        this.members = members;
        this.accounts = accounts;
        this.events = events;
    }

    @Override
    public Optional<Household> findDefaultHousehold() {
        return households.findAll().stream()
                .min((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(h -> new Household(h.getId(), h.getName(), h.getTimezone()));
    }

    @Override
    public List<HouseholdMember> members(UUID householdId) {
        return members.findByHouseholdId(householdId).stream()
                .map(
                        m ->
                                new HouseholdMember(
                                        m.getId(),
                                        m.getDisplayName(),
                                        m.getInitial(),
                                        m.getColorHex(),
                                        m.getKind(),
                                        m.getRole(),
                                        m.isResponsibleCapable(),
                                        m.getAgeLabel(),
                                        m.getSortOrder()))
                .toList();
    }

    @Override
    public List<ConnectedAccount> connectedAccounts(UUID householdId) {
        return accounts.findByHouseholdId(householdId).stream()
                .map(
                        a ->
                                new ConnectedAccount(
                                        a.getId(), a.getProvider(), a.getLabel(), a.getStatus()))
                .toList();
    }

    @Override
    public List<CalendarEventData> findByHousehold(UUID householdId) {
        return events.findByHouseholdId(householdId).stream()
                .map(JpaCalendarAdapter::toDomain)
                .toList();
    }

    @Override
    public CalendarEventData save(CalendarEventData e) {
        CalendarEventEntity entity =
                new CalendarEventEntity(
                        e.id(),
                        e.householdId(),
                        e.title(),
                        e.recurring(),
                        e.dayOfWeek(),
                        e.eventDate(),
                        e.startMinute(),
                        e.endMinute(),
                        e.allDay(),
                        e.responsibleMemberId(),
                        e.location(),
                        e.needsDriver(),
                        Instant.now(),
                        new LinkedHashSet<>(e.ownerMemberIds()));
        return toDomain(events.save(entity));
    }

    private static CalendarEventData toDomain(CalendarEventEntity e) {
        return new CalendarEventData(
                e.getId(),
                e.getHouseholdId(),
                e.getTitle(),
                e.isRecurring(),
                e.getDayOfWeek(),
                e.getEventDate(),
                e.getStartMinute(),
                e.getEndMinute(),
                e.isAllDay(),
                List.copyOf(e.getOwnerMemberIds()),
                e.getResponsibleMemberId(),
                e.getLocation(),
                e.isNeedsDriver());
    }
}

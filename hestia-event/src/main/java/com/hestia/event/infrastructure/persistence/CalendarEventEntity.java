package com.hestia.event.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** JPA entity for the {@code calendar_event} table (V002), with owner ids in {@code event_owner}. */
@Entity
@Table(name = "calendar_event")
public class CalendarEventEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean recurring;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "start_minute")
    private Integer startMinute;

    @Column(name = "end_minute")
    private Integer endMinute;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Column(name = "responsible_member_id")
    private UUID responsibleMemberId;

    @Column private String location;

    @Column(name = "needs_driver", nullable = false)
    private boolean needsDriver;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_owner", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "member_id", nullable = false)
    private Set<UUID> ownerMemberIds = new LinkedHashSet<>();

    protected CalendarEventEntity() {}

    public CalendarEventEntity(
            UUID id,
            UUID householdId,
            String title,
            boolean recurring,
            Integer dayOfWeek,
            LocalDate eventDate,
            Integer startMinute,
            Integer endMinute,
            boolean allDay,
            UUID responsibleMemberId,
            String location,
            boolean needsDriver,
            Instant createdAt,
            Set<UUID> ownerMemberIds) {
        this.id = id;
        this.householdId = householdId;
        this.title = title;
        this.recurring = recurring;
        this.dayOfWeek = dayOfWeek;
        this.eventDate = eventDate;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.allDay = allDay;
        this.responsibleMemberId = responsibleMemberId;
        this.location = location;
        this.needsDriver = needsDriver;
        this.createdAt = createdAt;
        this.ownerMemberIds = new LinkedHashSet<>(ownerMemberIds);
    }

    public UUID getId() {
        return id;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public String getTitle() {
        return title;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public Integer getStartMinute() {
        return startMinute;
    }

    public Integer getEndMinute() {
        return endMinute;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public UUID getResponsibleMemberId() {
        return responsibleMemberId;
    }

    public String getLocation() {
        return location;
    }

    public boolean isNeedsDriver() {
        return needsDriver;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<UUID> getOwnerMemberIds() {
        return ownerMemberIds;
    }
}

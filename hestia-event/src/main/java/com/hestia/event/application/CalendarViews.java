package com.hestia.event.application;

import com.hestia.event.domain.CalendarRange;
import com.hestia.event.domain.MemberKind;
import com.hestia.event.domain.MemberRole;
import com.hestia.event.domain.SyncStatus;
import java.util.List;

/**
 * GraphQL response DTOs (the read model). Component names match the schema fields in
 * {@code graphql/types/calendar.graphqls} so Spring for GraphQL maps them automatically.
 * All values (time labels, coverage-gap flags, load percentages, colors) are computed in the
 * backend — the frontend only renders these.
 */
public final class CalendarViews {

    private CalendarViews() {}

    public record HouseholdCalendarView(
            HouseholdView household,
            CalendarPeriodView period,
            List<MemberView> members,
            List<EventView> events,
            List<CoverageGapView> coverageGaps,
            WeeklyLoadView load,
            List<ConnectedAccountView> connectedAccounts) {}

    public record HouseholdView(String id, String name, String timezone) {}

    public record CalendarPeriodView(
            CalendarRange range, String start, String end, String label, String timezone) {}

    public record MemberView(
            String id,
            String displayName,
            String initial,
            String colorHex,
            MemberKind kind,
            MemberRole role,
            boolean isResponsibleCapable,
            String ageLabel) {}

    public record EventView(
            String id,
            String title,
            String start,
            String end,
            boolean allDay,
            String timeLabel,
            int dayOfWeek,
            String date,
            List<MemberView> ownerMembers,
            MemberView responsibleMember,
            String colorHex,
            String location,
            boolean needsDriver,
            boolean isCoverageGap) {}

    public record CoverageGapView(String eventId, String label, String shortLabel) {}

    public record WeeklyLoadView(int total, String summaryLabel, List<LoadEntryView> entries) {}

    public record LoadEntryView(MemberView member, int count, int percent) {}

    public record ConnectedAccountView(
            String id, String provider, String label, SyncStatus status, String statusLabel) {}
}

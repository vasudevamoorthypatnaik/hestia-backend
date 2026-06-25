package com.hestia.event.application;

import com.hestia.event.application.CalendarViews.CalendarPeriodView;
import com.hestia.event.application.CalendarViews.ConnectedAccountView;
import com.hestia.event.application.CalendarViews.CoverageGapView;
import com.hestia.event.application.CalendarViews.EventView;
import com.hestia.event.application.CalendarViews.HouseholdCalendarView;
import com.hestia.event.application.CalendarViews.HouseholdView;
import com.hestia.event.application.CalendarViews.LoadEntryView;
import com.hestia.event.application.CalendarViews.MemberView;
import com.hestia.event.application.CalendarViews.WeeklyLoadView;
import com.hestia.event.domain.CalendarEventData;
import com.hestia.event.domain.CalendarRange;
import com.hestia.event.domain.ConnectedAccount;
import com.hestia.event.domain.Household;
import com.hestia.event.domain.HouseholdMember;
import com.hestia.event.domain.MemberKind;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the household-calendar read model and validates/persists new events. All computed values
 * — period window, projected event instants, time labels, coverage-gap flags, weekly-load split —
 * are produced HERE (backend), so the frontend is a pure renderer (TAC-2).
 */
@Service
public class HouseholdCalendarServiceImpl implements HouseholdCalendarService {

    private final HouseholdRepository households;
    private final CalendarEventRepository events;

    public HouseholdCalendarServiceImpl(
            HouseholdRepository households, CalendarEventRepository events) {
        this.households = households;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public HouseholdCalendarView getCalendar(String anchorIso, CalendarRange range) {
        Household household =
                households
                        .findDefaultHousehold()
                        .orElseThrow(() -> new IllegalStateException("No seeded household"));
        ZoneId zone = ZoneId.of(household.timezone());
        LocalDate anchor = LocalDate.parse(anchorIso);
        LocalDate start = range == CalendarRange.WEEK ? anchor.with(DayOfWeek.MONDAY) : anchor;
        LocalDate end = range == CalendarRange.WEEK ? start.plusDays(6) : anchor;

        List<HouseholdMember> members =
                households.members(household.id()).stream()
                        .sorted(Comparator.comparingInt(HouseholdMember::sortOrder))
                        .toList();
        Map<UUID, HouseholdMember> byId =
                members.stream().collect(Collectors.toMap(HouseholdMember::id, m -> m));
        Map<UUID, MemberView> viewById =
                members.stream()
                        .collect(
                                Collectors.toMap(
                                        HouseholdMember::id,
                                        HouseholdCalendarServiceImpl::toMemberView));

        List<CalendarEventData> stored = events.findByHousehold(household.id());

        // Project every stored event onto each date in the window it occurs on.
        List<EventView> projected = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            for (CalendarEventData e : stored) {
                if (occursOn(e, d)) {
                    projected.add(toEventView(e, d, zone, byId, viewById));
                }
            }
        }
        projected.sort(
                Comparator.comparing(EventView::date)
                        .thenComparingInt(ev -> ev.allDay() ? 0 : 1)
                        .thenComparing(ev -> ev.start() == null ? "" : ev.start()));

        List<CoverageGapView> gaps =
                projected.stream()
                        .filter(EventView::isCoverageGap)
                        .map(HouseholdCalendarServiceImpl::toCoverageGap)
                        .toList();

        WeeklyLoadView load = computeLoad(projected, members);

        List<ConnectedAccountView> accounts =
                households.connectedAccounts(household.id()).stream()
                        .map(HouseholdCalendarServiceImpl::toAccountView)
                        .toList();

        CalendarPeriodView period =
                new CalendarPeriodView(
                        range,
                        start.toString(),
                        end.toString(),
                        periodLabel(range, start, end),
                        household.timezone());

        return new HouseholdCalendarView(
                new HouseholdView(
                        household.id().toString(), household.name(), household.timezone()),
                period,
                members.stream().map(HouseholdCalendarServiceImpl::toMemberView).toList(),
                projected,
                gaps,
                load,
                accounts);
    }

    @Override
    @Transactional
    public EventView createEvent(CreateEventCommand cmd) {
        Household household =
                households
                        .findDefaultHousehold()
                        .orElseThrow(() -> new IllegalStateException("No seeded household"));
        ZoneId zone = ZoneId.of(household.timezone());
        List<HouseholdMember> members = households.members(household.id());
        Map<UUID, HouseholdMember> byId =
                members.stream().collect(Collectors.toMap(HouseholdMember::id, m -> m));
        Map<UUID, MemberView> viewById =
                members.stream()
                        .collect(
                                Collectors.toMap(
                                        HouseholdMember::id,
                                        HouseholdCalendarServiceImpl::toMemberView));

        validate(cmd, byId);

        Integer startMin = cmd.allDay() ? null : parseMinutes(cmd.startTime());
        Integer endMin = cmd.allDay() || cmd.endTime() == null ? null : parseMinutes(cmd.endTime());

        CalendarEventData toSave =
                new CalendarEventData(
                        UUID.randomUUID(),
                        household.id(),
                        cmd.title().trim(),
                        false,
                        null,
                        cmd.date(),
                        startMin,
                        endMin,
                        cmd.allDay(),
                        List.copyOf(cmd.ownerMemberIds()),
                        cmd.responsibleMemberId(),
                        blankToNull(cmd.location()),
                        cmd.needsDriver());
        CalendarEventData saved = events.save(toSave);
        return toEventView(saved, cmd.date(), zone, byId, viewById);
    }

    // ----- validation -----

    private void validate(CreateEventCommand cmd, Map<UUID, HouseholdMember> byId) {
        if (cmd.title() == null || cmd.title().isBlank()) {
            throw new InvalidEventException("Title is required.");
        }
        if (cmd.date() == null) {
            throw new InvalidEventException("Date is required.");
        }
        if (cmd.ownerMemberIds() == null || cmd.ownerMemberIds().isEmpty()) {
            throw new InvalidEventException("At least one owner is required.");
        }
        for (UUID owner : cmd.ownerMemberIds()) {
            if (!byId.containsKey(owner)) {
                throw new InvalidEventException("Owner is not a member of this household.");
            }
        }
        if (cmd.responsibleMemberId() != null) {
            HouseholdMember resp = byId.get(cmd.responsibleMemberId());
            if (resp == null) {
                throw new InvalidEventException(
                        "Responsible adult is not a member of this household.");
            }
            if (resp.kind() != MemberKind.ADULT) {
                throw new InvalidEventException("Responsible adult must be an adult.");
            }
        }
        if (!cmd.allDay()) {
            int s = parseMinutes(cmd.startTime());
            if (cmd.endTime() != null) {
                int e = parseMinutes(cmd.endTime());
                if (e <= s) {
                    throw new InvalidEventException("End time must be after start time.");
                }
            }
        }
    }

    // ----- projection helpers -----

    private static boolean occursOn(CalendarEventData e, LocalDate d) {
        if (e.recurring()) {
            return e.dayOfWeek() != null && e.dayOfWeek() == d.getDayOfWeek().getValue();
        }
        return d.equals(e.eventDate());
    }

    private static EventView toEventView(
            CalendarEventData e,
            LocalDate d,
            ZoneId zone,
            Map<UUID, HouseholdMember> byId,
            Map<UUID, MemberView> viewById) {
        List<MemberView> owners =
                e.ownerMemberIds().stream().filter(viewById::containsKey).map(viewById::get).toList();
        MemberView responsible =
                e.responsibleMemberId() == null ? null : viewById.get(e.responsibleMemberId());
        boolean coverageGap = e.responsibleMemberId() == null && e.needsDriver();
        String colorHex =
                owners.isEmpty() ? "#C4603D" : owners.get(0).colorHex(); // primary owner color
        String startIso =
                e.allDay() || e.startMinute() == null
                        ? null
                        : d.atTime(e.startMinute() / 60, e.startMinute() % 60)
                                .atZone(zone)
                                .toInstant()
                                .toString();
        String endIso =
                e.allDay() || e.endMinute() == null
                        ? null
                        : d.atTime(e.endMinute() / 60, e.endMinute() % 60)
                                .atZone(zone)
                                .toInstant()
                                .toString();
        return new EventView(
                e.id().toString(),
                e.title(),
                startIso,
                endIso,
                e.allDay(),
                timeLabel(e),
                d.getDayOfWeek().getValue(),
                d.toString(),
                owners,
                responsible,
                colorHex,
                e.location(),
                e.needsDriver(),
                coverageGap);
    }

    private static CoverageGapView toCoverageGap(EventView ev) {
        String dayShort =
                LocalDate.parse(ev.date())
                        .getDayOfWeek()
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        String time = ev.allDay() ? "all-day" : ev.timeLabel();
        // The title already names the subject (e.g. "Pickup — Maya"); don't re-prepend the owner.
        String label = dayShort + " " + time + " — " + ev.title() + " has no responsible adult.";
        String shortLabel = dayShort + " " + time + " — " + ev.title() + " · unassigned";
        return new CoverageGapView(ev.id(), label, shortLabel);
    }

    private static WeeklyLoadView computeLoad(List<EventView> projected, List<HouseholdMember> members) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, MemberView> adultViews = new LinkedHashMap<>();
        for (HouseholdMember m : members) {
            if (m.kind() == MemberKind.ADULT) {
                counts.put(m.id().toString(), 0);
                adultViews.put(m.id().toString(), toMemberView(m));
            }
        }
        int total = 0;
        for (EventView ev : projected) {
            if (ev.responsibleMember() != null && counts.containsKey(ev.responsibleMember().id())) {
                counts.merge(ev.responsibleMember().id(), 1, Integer::sum);
                total++;
            }
        }
        final int totalF = total;
        List<LoadEntryView> entries =
                counts.entrySet().stream()
                        .filter(en -> en.getValue() > 0)
                        .map(
                                en ->
                                        new LoadEntryView(
                                                adultViews.get(en.getKey()),
                                                en.getValue(),
                                                totalF == 0 ? 0 : Math.round(en.getValue() * 100f / totalF)))
                        .sorted(Comparator.comparingInt(LoadEntryView::count).reversed())
                        .toList();
        String summary = null;
        if (entries.size() >= 2 && entries.get(0).count() > entries.get(1).count()) {
            summary = entries.get(0).member().displayName() + " is carrying a bit more this week.";
        }
        return new WeeklyLoadView(total, summary, entries);
    }

    private static ConnectedAccountView toAccountView(ConnectedAccount a) {
        String statusLabel = a.status() == com.hestia.event.domain.SyncStatus.SYNCED
                ? "synced"
                : "Disconnected";
        return new ConnectedAccountView(
                a.id().toString(), a.provider(), a.label(), a.status(), statusLabel);
    }

    private static MemberView toMemberView(HouseholdMember m) {
        return new MemberView(
                m.id().toString(),
                m.displayName(),
                m.initial(),
                m.colorHex(),
                m.kind(),
                m.role(),
                m.responsibleCapable(),
                m.ageLabel());
    }

    // ----- formatting -----

    private static String timeLabel(CalendarEventData e) {
        if (e.allDay() || e.startMinute() == null) {
            return "All day";
        }
        if (e.endMinute() == null) {
            return hm(e.startMinute());
        }
        return hm(e.startMinute()) + " – " + hm(e.endMinute());
    }

    /** 24h minutes -> 12h numerals without am/pm, e.g. 960 -> "4:00". */
    private static String hm(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        int h12 = ((h + 11) % 12) + 1;
        return h12 + ":" + (m < 10 ? "0" + m : Integer.toString(m));
    }

    private static String periodLabel(CalendarRange range, LocalDate start, LocalDate end) {
        if (range == CalendarRange.DAY) {
            return start.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    + " "
                    + start.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    + " "
                    + start.getDayOfMonth();
        }
        String mon = start.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        String endMon = end.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        boolean sameMonth = start.getMonth() == end.getMonth();
        return sameMonth
                ? mon + " " + start.getDayOfMonth() + " – " + end.getDayOfMonth() + ", " + end.getYear()
                : mon + " " + start.getDayOfMonth() + " – " + endMon + " " + end.getDayOfMonth() + ", "
                        + end.getYear();
    }

    private static int parseMinutes(String hhmm) {
        if (hhmm == null) {
            throw new InvalidEventException("Start time is required for a timed event.");
        }
        String[] parts = hhmm.split(":");
        if (parts.length != 2) {
            throw new InvalidEventException("Time must be HH:mm.");
        }
        try {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) {
                throw new InvalidEventException("Time out of range.");
            }
            return h * 60 + m;
        } catch (NumberFormatException ex) {
            throw new InvalidEventException("Time must be HH:mm.");
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}

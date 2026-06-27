package com.hestia.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.hestia.event.application.CalendarViews.EventView;
import com.hestia.event.application.CalendarViews.HouseholdCalendarView;
import com.hestia.event.domain.CalendarEventData;
import com.hestia.event.domain.CalendarRange;
import com.hestia.event.domain.ConnectedAccount;
import com.hestia.event.domain.Household;
import com.hestia.event.domain.HouseholdMember;
import com.hestia.event.domain.MemberKind;
import com.hestia.event.domain.MemberRole;
import com.hestia.event.domain.SyncStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HouseholdCalendarServiceTest {

    private static final UUID HH = UUID.fromString("00000000-0000-0000-0000-0000000ca1e0");
    private static final UUID PALLAVI = UUID.fromString("00000000-0000-0000-0000-00000000a001");
    private static final UUID VASU = UUID.fromString("00000000-0000-0000-0000-00000000a002");
    private static final UUID MAYA = UUID.fromString("00000000-0000-0000-0000-00000000a003");

    @Mock private HouseholdRepository households;
    @Mock private CalendarEventRepository events;
    private HouseholdCalendarServiceImpl service;

    private final Household household = new Household(HH, "The Hearth", "America/Los_Angeles");
    private final HouseholdMember pallavi =
            new HouseholdMember(PALLAVI, "Pallavi", "P", "#C4603D", MemberKind.ADULT, MemberRole.ADMIN, true, null, 0);
    private final HouseholdMember vasu =
            new HouseholdMember(VASU, "Vasu", "V", "#5B7C99", MemberKind.ADULT, MemberRole.MEMBER, true, null, 1);
    private final HouseholdMember maya =
            new HouseholdMember(MAYA, "Maya", "M", "#B6843C", MemberKind.CHILD, MemberRole.NONE, false, "8", 2);

    @BeforeEach
    void setUp() {
        service = new HouseholdCalendarServiceImpl(households, events);
        // lenient: the malformed-anchor test throws before members() is reached.
        org.mockito.Mockito.lenient()
                .when(households.findDefaultHousehold())
                .thenReturn(java.util.Optional.of(household));
        org.mockito.Mockito.lenient()
                .when(households.members(HH))
                .thenReturn(List.of(pallavi, vasu, maya));
    }

    @Test
    void projectsRecurringEventsOntoRequestedWeekWithBackendComputedFields() {
        // Tuesday soccer (Maya owner, Vasu responsible) + Friday unassigned pickup (coverage gap)
        when(events.findByHousehold(HH))
                .thenReturn(
                        List.of(
                                recurring(2, "Soccer — Maya", 960, 1050, VASU, MAYA, true),
                                recurring(5, "Pickup — Maya", 930, null, null, MAYA, true)));
        when(households.connectedAccounts(HH)).thenReturn(List.of());

        // anchor mid-week; service resolves Mon–Sun window
        HouseholdCalendarView view = service.getCalendar("2026-06-24", CalendarRange.WEEK);

        assertThat(view.period().start()).isEqualTo("2026-06-22"); // Monday
        assertThat(view.period().end()).isEqualTo("2026-06-28"); // Sunday
        assertThat(view.events()).hasSize(2);

        EventView soccer =
                view.events().stream().filter(e -> e.title().equals("Soccer — Maya")).findFirst().orElseThrow();
        assertThat(soccer.timeLabel()).isEqualTo("4:00 – 5:30"); // backend-formatted
        assertThat(soccer.colorHex()).isEqualTo("#B6843C"); // owner (Maya) color
        assertThat(soccer.responsibleMember().displayName()).isEqualTo("Vasu");
        assertThat(soccer.dayOfWeek()).isEqualTo(2);
        assertThat(soccer.isCoverageGap()).isFalse();
        assertThat(soccer.start()).isNotNull();

        EventView pickup =
                view.events().stream().filter(e -> e.title().equals("Pickup — Maya")).findFirst().orElseThrow();
        assertThat(pickup.isCoverageGap()).isTrue();
        assertThat(view.coverageGaps()).hasSize(1);
        assertThat(view.coverageGaps().get(0).shortLabel()).contains("unassigned");
    }

    @Test
    void computesWeeklyLoadSplitInBackend() {
        when(events.findByHousehold(HH))
                .thenReturn(
                        List.of(
                                recurring(1, "A", 540, null, PALLAVI, MAYA, false),
                                recurring(2, "B", 540, null, VASU, MAYA, false),
                                recurring(3, "C", 540, null, VASU, MAYA, false)));
        when(households.connectedAccounts(HH)).thenReturn(List.of());

        HouseholdCalendarView view = service.getCalendar("2026-06-24", CalendarRange.WEEK);

        assertThat(view.load().total()).isEqualTo(3);
        assertThat(view.load().entries()).hasSize(2);
        // Vasu (2) sorted before Pallavi (1)
        assertThat(view.load().entries().get(0).member().displayName()).isEqualTo("Vasu");
        assertThat(view.load().entries().get(0).count()).isEqualTo(2);
        assertThat(view.load().entries().get(0).percent()).isEqualTo(67);
        assertThat(view.load().summaryLabel()).contains("Vasu");
    }

    @Test
    void dayRangeReturnsOnlyThatDay() {
        when(events.findByHousehold(HH))
                .thenReturn(
                        List.of(
                                recurring(2, "Tue event", 600, null, VASU, MAYA, false),
                                recurring(4, "Thu event", 600, null, VASU, MAYA, false)));
        when(households.connectedAccounts(HH)).thenReturn(List.of());

        // 2026-06-25 is a Thursday
        HouseholdCalendarView view = service.getCalendar("2026-06-25", CalendarRange.DAY);

        assertThat(view.events()).extracting(EventView::title).containsExactly("Thu event");
        assertThat(view.period().label()).contains("Thursday");
    }

    @Test
    void createEventValidatesAndPersists() {
        when(events.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventView created =
                service.createEvent(
                        new CreateEventCommand(
                                "Dentist — Maya",
                                LocalDate.parse("2026-06-24"),
                                "15:00",
                                "15:45",
                                false,
                                List.of(MAYA),
                                PALLAVI,
                                true,
                                null));

        ArgumentCaptor<CalendarEventData> captor = ArgumentCaptor.forClass(CalendarEventData.class);
        org.mockito.Mockito.verify(events).save(captor.capture());
        CalendarEventData saved = captor.getValue();
        assertThat(saved.recurring()).isFalse();
        assertThat(saved.eventDate()).isEqualTo(LocalDate.parse("2026-06-24"));
        assertThat(saved.startMinute()).isEqualTo(900);
        assertThat(saved.endMinute()).isEqualTo(945);
        assertThat(created.timeLabel()).isEqualTo("3:00 – 3:45");
        assertThat(created.responsibleMember().displayName()).isEqualTo("Pallavi");
        assertThat(created.isCoverageGap()).isFalse();
    }

    @Test
    void createEventRejectsBlankTitle() {
        assertThatThrownBy(
                        () ->
                                service.createEvent(
                                        new CreateEventCommand(
                                                "  ", LocalDate.parse("2026-06-24"), "10:00", null, false,
                                                List.of(MAYA), null, false, null)))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("Title");
    }

    @Test
    void createEventRejectsNoOwners() {
        assertThatThrownBy(
                        () ->
                                service.createEvent(
                                        new CreateEventCommand(
                                                "X", LocalDate.parse("2026-06-24"), "10:00", null, false,
                                                List.of(), null, false, null)))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("owner");
    }

    @Test
    void createEventRejectsChildAsResponsibleAdult() {
        assertThatThrownBy(
                        () ->
                                service.createEvent(
                                        new CreateEventCommand(
                                                "X", LocalDate.parse("2026-06-24"), "10:00", null, false,
                                                List.of(MAYA), MAYA, false, null)))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("adult");
    }

    @Test
    void createEventRejectsNonCapableResponsibleAdult() {
        UUID granId = UUID.fromString("00000000-0000-0000-0000-0000000000cc");
        HouseholdMember gran =
                new HouseholdMember(
                        granId, "Gran", "G", "#999999", MemberKind.ADULT, MemberRole.MEMBER, false, null, 5);
        when(households.members(HH)).thenReturn(List.of(pallavi, gran, maya));
        assertThatThrownBy(
                        () ->
                                service.createEvent(
                                        new CreateEventCommand(
                                                "X", LocalDate.parse("2026-06-24"), "10:00", null, false,
                                                List.of(MAYA), granId, false, null)))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("cannot be assigned");
    }

    @Test
    void createEventRejectsOverlongTitle() {
        String longTitle = "x".repeat(201);
        assertThatThrownBy(
                        () ->
                                service.createEvent(
                                        new CreateEventCommand(
                                                longTitle, LocalDate.parse("2026-06-24"), "10:00", null,
                                                false, List.of(MAYA), null, false, null)))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("200");
    }

    @Test
    void getCalendarRejectsMalformedAnchor() {
        assertThatThrownBy(() -> service.getCalendar("not-a-date", CalendarRange.WEEK))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("ISO");
    }

    @Test
    void createEventRejectsEndBeforeStart() {
        assertThatThrownBy(
                        () ->
                                service.createEvent(
                                        new CreateEventCommand(
                                                "X", LocalDate.parse("2026-06-24"), "12:00", "11:00", false,
                                                List.of(MAYA), null, false, null)))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("End time");
    }

    @Test
    void monthRangeWindowSpansFirstThroughLastDayWithMonthLabel() {
        when(events.findByHousehold(HH)).thenReturn(List.of());
        when(households.connectedAccounts(HH)).thenReturn(List.of());

        // anchor mid-month; service resolves the full calendar month window.
        HouseholdCalendarView view = service.getCalendar("2026-06-15", CalendarRange.MONTH);

        assertThat(view.period().start()).isEqualTo("2026-06-01"); // first day
        assertThat(view.period().end()).isEqualTo("2026-06-30"); // last day
        assertThat(view.period().label()).isEqualTo("June 2026");
    }

    @Test
    void monthRangeWindowEndClampsToShortMonth() {
        when(events.findByHousehold(HH)).thenReturn(List.of());
        when(households.connectedAccounts(HH)).thenReturn(List.of());

        // February 2026 is a non-leap month — the window must end on the 28th, not spill over.
        HouseholdCalendarView view = service.getCalendar("2026-02-10", CalendarRange.MONTH);

        assertThat(view.period().start()).isEqualTo("2026-02-01");
        assertThat(view.period().end()).isEqualTo("2026-02-28");
        assertThat(view.period().label()).isEqualTo("February 2026");
    }

    @Test
    void monthRangeFansWeeklyRecurringEventOntoEveryMatchingWeekday() {
        // A Tuesday-recurring event must project onto every Tuesday in the month (R11): the
        // projection loop is range-agnostic, so a MONTH window fans one weekly event across ~4–5
        // concrete dates rather than collapsing to a single week.
        when(events.findByHousehold(HH))
                .thenReturn(List.of(recurring(2, "Soccer — Maya", 960, 1050, VASU, MAYA, true)));
        when(households.connectedAccounts(HH)).thenReturn(List.of());

        HouseholdCalendarView view = service.getCalendar("2026-06-15", CalendarRange.MONTH);

        // June 2026 Tuesdays: 2, 9, 16, 23, 30 -> five projected instances on distinct dates.
        assertThat(view.events())
                .extracting(EventView::date)
                .containsExactly(
                        "2026-06-02", "2026-06-09", "2026-06-16", "2026-06-23", "2026-06-30");
        assertThat(view.events()).allSatisfy(e -> assertThat(e.title()).isEqualTo("Soccer — Maya"));
    }

    @Test
    void connectedAccountsCarryStatusLabel() {
        when(events.findByHousehold(HH)).thenReturn(List.of());
        when(households.connectedAccounts(HH))
                .thenReturn(
                        List.of(
                                new ConnectedAccount(
                                        UUID.randomUUID(), "Google", "Google · Pallavi", SyncStatus.SYNCED)));

        HouseholdCalendarView view = service.getCalendar("2026-06-24", CalendarRange.WEEK);
        assertThat(view.connectedAccounts()).hasSize(1);
        assertThat(view.connectedAccounts().get(0).statusLabel()).isEqualTo("synced");
    }

    // helper
    private static CalendarEventData recurring(
            int dow, String title, Integer start, Integer end, UUID responsible, UUID owner, boolean needsDriver) {
        return new CalendarEventData(
                UUID.randomUUID(), HH, title, true, dow, null, start, end, false,
                List.of(owner), responsible, null, needsDriver);
    }
}

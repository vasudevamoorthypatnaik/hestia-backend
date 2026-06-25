package com.hestia.event.application;

import com.hestia.event.application.CalendarViews.EventView;
import com.hestia.event.application.CalendarViews.HouseholdCalendarView;
import com.hestia.event.domain.CalendarRange;

/** Read + write use cases for the household calendar. */
public interface HouseholdCalendarService {

    /** Build the calendar view for the requested period (window resolved in the household tz). */
    HouseholdCalendarView getCalendar(String anchorIso, CalendarRange range);

    /** Validate + persist a new event; returns the created event projected on its date. */
    EventView createEvent(CreateEventCommand command);
}

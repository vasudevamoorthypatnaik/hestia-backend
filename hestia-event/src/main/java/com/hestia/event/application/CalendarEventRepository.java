package com.hestia.event.application;

import com.hestia.event.domain.CalendarEventData;
import java.util.List;
import java.util.UUID;

/** Port for calendar-event reads and writes. */
public interface CalendarEventRepository {

    List<CalendarEventData> findByHousehold(UUID householdId);

    CalendarEventData save(CalendarEventData event);
}

package com.hestia.event.domain;

/**
 * The window a calendar view spans: a single DAY (mobile), a Mon–Sun WEEK (web week strip), or a
 * full calendar MONTH (web month grid — first-day through last-day of the anchor's month).
 */
public enum CalendarRange {
    DAY,
    WEEK,
    MONTH
}

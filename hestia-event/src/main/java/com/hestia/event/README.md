# Event Domain (Household Calendar)

**Feature(s):** HES-CAL (Household Calendar), calender-view-shwoing-data (MONTH range)

## Requirements

The event domain owns the **household calendar** read model and the **create-event** write path.
It builds a fully-computed view of a household's schedule for a window (DAY, WEEK, or MONTH) and
validates/persists new calendar events. Per the backend-is-source-of-all-logic rule, **every**
display value — period window, projected event instants, time labels, coverage-gap flags,
weekly-load split, member colors — is computed here; the frontend is a pure renderer.

### Use cases
- **View the household calendar** for a window anchored on an ISO date:
  - `DAY` — a single day (mobile agenda).
  - `WEEK` — the Mon–Sun week containing the anchor (web week strip).
  - `MONTH` — the full calendar month containing the anchor, first day through last day (web month grid).
- **Create a calendar event** — validated server-side, persisted, and returned as a projected `EventView`.

## Domain Model

**Entities / value objects** (`domain/`):
- `CalendarEventData` — a stored event (date or weekly-recurring), owners, responsible adult, location, needsDriver.
- `Household`, `HouseholdMember`, `ConnectedAccount` — the household and its people/integrations.
- `CalendarRange` — `DAY | WEEK | MONTH` window selector.
- `MemberKind` (`ADULT | CHILD`), `MemberRole` (`ADMIN | MEMBER | NONE`), `SyncStatus`.

**Application** (`application/`):
- `HouseholdCalendarService` / `…Impl` — builds the read model (`getCalendar`) and creates events (`createEvent`).
- `CalendarViews` — the GraphQL response DTOs (record names match the schema fields).
- `CreateEventCommand`, `InvalidEventException`, `UnauthenticatedException`.

**Infrastructure** (`infrastructure/`):
- `graphql/HouseholdCalendarQueryResolver` — the `householdCalendar` query (auth-gated, TAC-1).
- `graphql/CalendarEventMutationResolver` — the `createCalendarEvent` mutation.
- `persistence/JpaCalendarAdapter` + `*JpaRepository` / `*Entity` — Postgres persistence.

## GraphQL API

### Query
```graphql
householdCalendar(period: CalendarPeriodInput!): HouseholdCalendar!
# CalendarPeriodInput { anchor: String!  range: CalendarRange! }
```

### Mutation
```graphql
createCalendarEvent(input: CreateCalendarEventInput!): CreateCalendarEventPayload!
```

### Enums
- `CalendarRange = DAY | WEEK | MONTH`

### Errors
- `UNAUTHORIZED` — no authenticated user (the `householdCalendar` query requires a token).
- `BAD_REQUEST` — invalid anchor / invalid event input (`InvalidEventException`).
- `INTERNAL_ERROR` — no seeded household (server misconfiguration).

## Window Computation (`HouseholdCalendarServiceImpl.getCalendar`)

The anchor's window is resolved in the **household timezone**:

| Range | Start | End |
|-------|-------|-----|
| `DAY` | anchor | anchor |
| `WEEK` | anchor's Monday | Monday + 6 |
| `MONTH` | first day of anchor's month | last day of anchor's month |

The projection loop is **range-agnostic**: it iterates `[start, end]` day-by-day and projects every
event that `occursOn` each date. A weekly-recurring event therefore naturally fans out onto each
matching weekday across the whole window — ~4–5 instances in a MONTH. The period label is:
`"<Weekday> <Month> <day>"` (DAY), `"<Mon> d – d, yyyy"` (WEEK), `"<Month> yyyy"` (MONTH).

## Database Schema

Tables created by `V002__create_household_calendar.sql`: `household`, `household_member`,
`calendar_event`, `event_owner`, `connected_account`. Dev seed: `V900__seed_hearth_household.sql`
("The Hearth"). **No schema or seed change** was required for the MONTH range — filtering/projection
is in-memory over `findByHousehold`, so MONTH only widens the in-memory window.

> **Demo scoping (known limitation):** a single seeded household is resolved via
> `findDefaultHousehold()`; there is no `authUserId → household` mapping yet (the
> `household_member.user_id` column is reserved for it). Safe only while exactly one household exists.

## Architecture Decisions

### ADR-1 — MONTH range is computed in the backend (not stitched on the client)
**Date:** 2026-06-26
**Context:** The web calendar needed a month grid. Two options: (A) add a backend `MONTH`
`CalendarRange`; (B) have the client fire N weekly queries and stitch the results.
**Decision:** (A). Month-boundary / timezone / DST computation is date/time logic and MUST live in
the backend (single source of truth across web + iOS + Android). Option (B) would duplicate that
computation on three client platforms and invite drift.
**Consequences:** Purely additive — one enum value (`MONTH`) plus a `MONTH` branch in the window
switch and `periodLabel`. DAY/WEEK clients are untouched (adding an enum value is backward-compatible
per GraphQL conventions §1.2). No DB migration, no seed change, no auth/household change. The
projection/load/createEvent paths are range-agnostic and were reused unchanged.

## Test Coverage
- `HouseholdCalendarServiceTest` (unit) — window + label per range, projection field correctness.
- `HouseholdCalendarIT` (integration, Testcontainers + Spring GraphQL) — the query/mutation paths,
  auth rejection, create-persists-and-appears, seed-not-duplicated.

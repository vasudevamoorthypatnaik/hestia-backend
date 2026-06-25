-- V002__create_household_calendar.sql (HES-CAL)
-- Household calendar domain: households, members, events (with owners), connected accounts.
-- Column names/types/nullability mirror the JPA entities so `ddl-auto: validate` boots clean.
-- Rollback: DROP TABLE event_owner, calendar_event, connected_account, household_member, household.

CREATE TABLE IF NOT EXISTS household (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS household_member (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES household (id) ON DELETE CASCADE,
    user_id UUID,
    display_name VARCHAR(120) NOT NULL,
    initial VARCHAR(4) NOT NULL,
    color_hex VARCHAR(9) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    role VARCHAR(16) NOT NULL,
    responsible_capable BOOLEAN NOT NULL DEFAULT FALSE,
    age_label VARCHAR(16),
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_household_member_household ON household_member (household_id);

CREATE TABLE IF NOT EXISTS calendar_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES household (id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    recurring BOOLEAN NOT NULL DEFAULT FALSE,
    day_of_week INTEGER,
    event_date DATE,
    start_minute INTEGER,
    end_minute INTEGER,
    all_day BOOLEAN NOT NULL DEFAULT FALSE,
    responsible_member_id UUID REFERENCES household_member (id) ON DELETE SET NULL,
    location VARCHAR(255),
    needs_driver BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_calendar_event_household ON calendar_event (household_id);

CREATE TABLE IF NOT EXISTS event_owner (
    event_id UUID NOT NULL REFERENCES calendar_event (id) ON DELETE CASCADE,
    member_id UUID NOT NULL REFERENCES household_member (id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, member_id)
);

CREATE TABLE IF NOT EXISTS connected_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL REFERENCES household (id) ON DELETE CASCADE,
    provider VARCHAR(40) NOT NULL,
    label VARCHAR(120) NOT NULL,
    status VARCHAR(16) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_connected_account_household ON connected_account (household_id);

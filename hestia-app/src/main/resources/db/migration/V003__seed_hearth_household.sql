-- V003__seed_hearth_household.sql (HES-CAL)
-- Idempotent seed of the shared demo household "The Hearth" (Pallavi/Vasu/Maya/Theo) and a week of
-- recurring events mirroring the FLOW 3 design. Events are stored as (day_of_week, minute-of-day)
-- so the resolver projects them onto whatever week is requested — the calendar always looks
-- "current". Fixed UUIDs + ON CONFLICT DO NOTHING => re-running on a populated DB does not
-- duplicate household/member/event rows (TAC-4).
-- Rollback: DELETE the rows by these fixed ids (event_owner, calendar_event, connected_account,
-- household_member, household).

-- Household (Pacific time)
INSERT INTO household (id, name, timezone, created_at) VALUES
    ('00000000-0000-0000-0000-0000000ca1e0', 'The Hearth', 'America/Los_Angeles', NOW())
ON CONFLICT (id) DO NOTHING;

-- Members: Pallavi (admin), Vasu (adult), Maya (child, 8), Theo (child, 5)
INSERT INTO household_member
    (id, household_id, user_id, display_name, initial, color_hex, kind, role, responsible_capable, age_label, sort_order)
VALUES
    ('00000000-0000-0000-0000-00000000a001', '00000000-0000-0000-0000-0000000ca1e0', NULL, 'Pallavi', 'P', '#C4603D', 'ADULT', 'ADMIN',  TRUE,  NULL, 0),
    ('00000000-0000-0000-0000-00000000a002', '00000000-0000-0000-0000-0000000ca1e0', NULL, 'Vasu',    'V', '#5B7C99', 'ADULT', 'MEMBER', TRUE,  NULL, 1),
    ('00000000-0000-0000-0000-00000000a003', '00000000-0000-0000-0000-0000000ca1e0', NULL, 'Maya',    'M', '#B6843C', 'CHILD', 'NONE',   FALSE, '8',  2),
    ('00000000-0000-0000-0000-00000000a004', '00000000-0000-0000-0000-0000000ca1e0', NULL, 'Theo',    'T', '#6E9466', 'CHILD', 'NONE',   FALSE, '5',  3)
ON CONFLICT (id) DO NOTHING;

-- Connected accounts (seed-only sync status)
INSERT INTO connected_account (id, household_id, provider, label, status) VALUES
    ('00000000-0000-0000-0000-0000000acc01', '00000000-0000-0000-0000-0000000ca1e0', 'Google', 'Google · Pallavi', 'SYNCED'),
    ('00000000-0000-0000-0000-0000000acc02', '00000000-0000-0000-0000-0000000ca1e0', 'iCloud', 'iCloud · Vasu',     'SYNCED')
ON CONFLICT (id) DO NOTHING;

-- Recurring weekly events (day_of_week: 1=Mon..7=Sun; minutes from midnight)
INSERT INTO calendar_event
    (id, household_id, title, recurring, day_of_week, event_date, start_minute, end_minute, all_day, responsible_member_id, location, needs_driver, created_at)
VALUES
    -- MON
    ('00000000-0000-0000-0000-00000000e001', '00000000-0000-0000-0000-0000000ca1e0', 'Standup', TRUE, 1, NULL, 540, NULL, FALSE, '00000000-0000-0000-0000-00000000a001', NULL, FALSE, NOW()),
    -- TUE
    ('00000000-0000-0000-0000-00000000e002', '00000000-0000-0000-0000-0000000ca1e0', 'Soccer — Maya', TRUE, 2, NULL, 960, 1050, FALSE, '00000000-0000-0000-0000-00000000a002', 'Lincoln Park', TRUE, NOW()),
    -- WED
    ('00000000-0000-0000-0000-00000000e003', '00000000-0000-0000-0000-0000000ca1e0', 'Pizza day', TRUE, 3, NULL, NULL, NULL, TRUE, NULL, 'School', FALSE, NOW()),
    ('00000000-0000-0000-0000-00000000e004', '00000000-0000-0000-0000-0000000ca1e0', 'Piano — Maya', TRUE, 3, NULL, 1020, 1065, FALSE, '00000000-0000-0000-0000-00000000a001', 'Home', FALSE, NOW()),
    -- THU
    ('00000000-0000-0000-0000-00000000e005', '00000000-0000-0000-0000-0000000ca1e0', 'Swim — Theo', TRUE, 4, NULL, 480, 510, FALSE, '00000000-0000-0000-0000-00000000a002', 'Aquatic Center', FALSE, NOW()),
    ('00000000-0000-0000-0000-00000000e006', '00000000-0000-0000-0000-0000000ca1e0', 'Soccer — Maya', TRUE, 4, NULL, 960, 1050, FALSE, '00000000-0000-0000-0000-00000000a002', 'Lincoln Park', TRUE, NOW()),
    -- FRI
    ('00000000-0000-0000-0000-00000000e007', '00000000-0000-0000-0000-0000000ca1e0', 'Early dismissal', TRUE, 5, NULL, 720, NULL, FALSE, NULL, 'School', FALSE, NOW()),
    ('00000000-0000-0000-0000-00000000e008', '00000000-0000-0000-0000-0000000ca1e0', 'Pickup — Maya', TRUE, 5, NULL, 930, NULL, FALSE, NULL, 'School', TRUE, NOW()),
    -- SAT
    ('00000000-0000-0000-0000-00000000e009', '00000000-0000-0000-0000-0000000ca1e0', 'Soccer game', TRUE, 6, NULL, 540, 630, FALSE, '00000000-0000-0000-0000-00000000a001', 'Lincoln Park', TRUE, NOW()),
    ('00000000-0000-0000-0000-00000000e00a', '00000000-0000-0000-0000-0000000ca1e0', 'Birthday party', TRUE, 6, NULL, 840, 960, FALSE, '00000000-0000-0000-0000-00000000a002', NULL, FALSE, NOW())
ON CONFLICT (id) DO NOTHING;

-- Event owners (whom each event is about)
INSERT INTO event_owner (event_id, member_id) VALUES
    ('00000000-0000-0000-0000-00000000e001', '00000000-0000-0000-0000-00000000a001'), -- Standup: Pallavi
    ('00000000-0000-0000-0000-00000000e002', '00000000-0000-0000-0000-00000000a003'), -- Soccer Tue: Maya
    ('00000000-0000-0000-0000-00000000e003', '00000000-0000-0000-0000-00000000a003'), -- Pizza: Maya
    ('00000000-0000-0000-0000-00000000e003', '00000000-0000-0000-0000-00000000a004'), -- Pizza: Theo
    ('00000000-0000-0000-0000-00000000e004', '00000000-0000-0000-0000-00000000a003'), -- Piano: Maya
    ('00000000-0000-0000-0000-00000000e005', '00000000-0000-0000-0000-00000000a004'), -- Swim: Theo
    ('00000000-0000-0000-0000-00000000e006', '00000000-0000-0000-0000-00000000a003'), -- Soccer Thu: Maya
    ('00000000-0000-0000-0000-00000000e007', '00000000-0000-0000-0000-00000000a003'), -- Early dismissal: Maya
    ('00000000-0000-0000-0000-00000000e007', '00000000-0000-0000-0000-00000000a004'), -- Early dismissal: Theo
    ('00000000-0000-0000-0000-00000000e008', '00000000-0000-0000-0000-00000000a003'), -- Pickup: Maya
    ('00000000-0000-0000-0000-00000000e009', '00000000-0000-0000-0000-00000000a003'), -- Soccer game: Maya
    ('00000000-0000-0000-0000-00000000e00a', '00000000-0000-0000-0000-00000000a004')  -- Birthday: Theo
ON CONFLICT (event_id, member_id) DO NOTHING;

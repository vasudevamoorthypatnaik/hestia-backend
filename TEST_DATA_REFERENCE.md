# Test Data Reference Guide

**Created:** 2026-02-16 (INV-20 - Frontend-Backend Integration)

This document describes the test seed data available for frontend-backend integration testing.

---

## Test Users

### Host (Event Organizer)
- **ID:** `11111111-1111-1111-1111-111111111111`
- **Name:** Alice Smith
- **Email:** alice.smith@test.com

### Guest (For Manual Testing)
- **ID:** `22222222-2222-2222-2222-222222222222`
- **Name:** Bob Johnson
- **Email:** bob.johnson@test.com

---

## Test Events

### 1. Summer BBQ Party (FUTURE - Active Invitations)
- **Event ID:** `33333333-3333-3333-3333-333333333333`
- **Name:** TEST: Summer BBQ Party 2026
- **Date:** July 15, 2026, 5:00 PM - 10:00 PM PT
- **Location:** Smith Residence, 123 Maple Street, San Francisco, CA 94102
- **RSVP Deadline:** July 8, 2026 (FUTURE - Valid for testing)
- **Host:** Alice Smith
- **Status:** CREATED
- **Dress Code:** Casual
- **Max Attendees:** 4 per invitation
- **Use Case:** Testing first-time RSVP submission

### 2. Alice & John Wedding (FUTURE - Active)
- **Event ID:** `55555555-5555-5555-5555-555555555555`
- **Name:** TEST: Alice & John Wedding
- **Date:** September 20, 2026, 3:00 PM - 11:00 PM ET
- **Location:** Garden Estate Venue, 456 Oak Avenue, New York, NY 10001
- **RSVP Deadline:** August 20, 2026 (FUTURE - Valid)
- **Host:** Alice Smith
- **Status:** CREATED
- **Dress Code:** Formal - Black tie optional
- **Max Attendees:** 2 per invitation
- **Use Case:** Testing invitations with existing RSVPs

### 3. 30th Birthday Bash (DEADLINE EXPIRED)
- **Event ID:** `77777777-7777-7777-7777-777777777777`
- **Name:** TEST: 30th Birthday Bash
- **Date:** August 10, 2026, 7:00 PM - 11:00 PM CT
- **Location:** Downtown Bar & Grill, 789 Main Street, Chicago, IL 60601
- **RSVP Deadline:** February 1, 2026 (PAST - Expired)
- **Host:** Alice Smith
- **Status:** CREATED
- **Max Attendees:** 3 per invitation
- **Use Case:** Testing expired deadline error handling

---

## Test Invitations

### 1. Summer BBQ - PENDING (Ready for RSVP)
- **Invitation ID:** `aaaaaaaa-0001-0001-0001-000000000001`
- **Event:** Summer BBQ Party
- **Guest:** Sarah Martinez (sarah.martinez@test.com)
- **Status:** PENDING
- **RSVP Deadline:** July 8, 2026 (FUTURE - Valid)
- **Max Attendees:** 4
- **Special Instructions:** "We have a pool, so bring swimsuits if you'd like!"
- **Use Case:** ✅ **PRIMARY TEST CASE** - First-time RSVP submission

**GraphQL Query to Fetch:**
```graphql
query GetPendingInvitation {
  invitation(id: "aaaaaaaa-0001-0001-0001-000000000001") {
    id
    status
    guestName
    guestEmail
    maxAttendeesPerInvitation
    rsvpDeadline
    specialInstructions
    event { id name date startTime endTime timezone location { name address city state } }
    host { id name email }
    rsvp { id status respondedAt guestCount { adults kids } }
  }
}
```

**Expected Result:**
- `status`: PENDING
- `rsvp`: null (no RSVP yet)
- Ready for RSVP submission

---

### 2. Wedding - ACCEPTED (Already Responded)
- **Invitation ID:** `aaaaaaaa-0002-0002-0002-000000000002`
- **Event:** Alice & John Wedding
- **Guest:** Michael Chen (michael.chen@test.com)
- **Status:** ACCEPTED
- **RSVP Status:** ACCEPTED (2 adults, 0 kids)
- **RSVP Message:** "So excited to celebrate with you! Congratulations!"
- **Dietary Restrictions:** One vegetarian meal needed
- **Use Case:** Testing duplicate RSVP error (already submitted)

**GraphQL Query to Fetch:**
```graphql
query GetAcceptedInvitation {
  invitation(id: "aaaaaaaa-0002-0002-0002-000000000002") {
    id
    status
    guestName
    rsvp {
      id
      status
      respondedAt
      guestCount { adults kids }
      dietaryRestrictions
      message
    }
  }
}
```

**Expected Result:**
- `status`: ACCEPTED
- `rsvp`: { status: ACCEPTED, adults: 2, kids: 0 }

**Test Duplicate RSVP:**
```graphql
mutation TryDuplicateRSVP {
  respondToInvitation(input: {
    invitationId: "aaaaaaaa-0002-0002-0002-000000000002"
    status: ACCEPTED
    guestCount: { adults: 1, kids: 0 }
  }) {
    invitation { id }
  }
}
```

**Expected Error:** `IllegalStateException: "RSVP already submitted..."`

---

### 3. Birthday - PENDING with EXPIRED DEADLINE
- **Invitation ID:** `aaaaaaaa-0003-0003-0003-000000000003`
- **Event:** 30th Birthday Bash
- **Guest:** Emily Rodriguez (emily.rodriguez@test.com)
- **Status:** PENDING
- **RSVP Deadline:** February 1, 2026 (PAST - Expired)
- **Use Case:** Testing expired deadline error

**Test Expired Deadline:**
```graphql
mutation TryExpiredRSVP {
  respondToInvitation(input: {
    invitationId: "aaaaaaaa-0003-0003-0003-000000000003"
    status: ACCEPTED
    guestCount: { adults: 1, kids: 0 }
  }) {
    invitation { id }
  }
}
```

**Expected Error:** `IllegalStateException: "RSVP deadline has passed. Deadline was 2026-02-01T23:59:59Z..."`

---

### 4. Summer BBQ - DECLINED
- **Invitation ID:** `aaaaaaaa-0004-0004-0004-000000000004`
- **Event:** Summer BBQ Party
- **Guest:** David Lee (david.lee@test.com)
- **Status:** DECLINED
- **RSVP Status:** DECLINED (0 adults, 0 kids)
- **RSVP Message:** "Sorry, we have a conflict that weekend. Have a great time!"
- **Use Case:** Testing declined invitation display

---

### 5. Wedding - TENTATIVE
- **Invitation ID:** `aaaaaaaa-0005-0005-0005-000000000005`
- **Event:** Alice & John Wedding
- **Guest:** Jessica Brown (jessica.brown@test.com)
- **Status:** TENTATIVE
- **RSVP Status:** TENTATIVE (1 adult, 1 kid)
- **RSVP Message:** "We're hoping to make it but have a work commitment that might conflict."
- **Dietary Restrictions:** Gluten-free needed
- **Use Case:** Testing tentative RSVP display

---

## Testing Scenarios

### Scenario 1: Submit First-Time RSVP (Happy Path)
**Use:** `aaaaaaaa-0001-0001-0001-000000000001`

1. Fetch invitation via GraphQL query
2. Verify `status: PENDING` and `rsvp: null`
3. Submit RSVP:
```graphql
mutation SubmitFirstRSVP {
  respondToInvitation(input: {
    invitationId: "aaaaaaaa-0001-0001-0001-000000000001"
    status: ACCEPTED
    guestCount: { adults: 2, kids: 1 }
    dietaryRestrictions: "One vegan meal needed"
    message: "Looking forward to it!"
  }) {
    invitation {
      id
      status
      rsvp {
        id
        status
        respondedAt
        guestCount { adults kids }
        dietaryRestrictions
        message
      }
    }
  }
}
```
4. Verify response:
   - `invitation.status`: ACCEPTED (updated from PENDING)
   - `invitation.rsvp.status`: ACCEPTED
   - `invitation.rsvp.guestCount`: { adults: 2, kids: 1 }

**Expected Frontend Behavior:**
- Show loading spinner during mutation
- Update invitation status to ACCEPTED
- Display success message
- Show RSVP details (adults: 2, kids: 1)

---

### Scenario 2: Duplicate RSVP Error
**Use:** `aaaaaaaa-0002-0002-0002-000000000002`

1. Fetch invitation (already has RSVP)
2. Try to submit another RSVP
3. Expect `IllegalStateException: "RSVP already submitted..."`

**Expected Frontend Behavior:**
- Display error toast: "You have already submitted your RSVP"
- Don't allow submission if RSVP exists
- Show current RSVP status

---

### Scenario 3: Expired Deadline Error
**Use:** `aaaaaaaa-0003-0003-0003-000000000003`

1. Fetch invitation (deadline passed)
2. Try to submit RSVP
3. Expect `IllegalStateException: "RSVP deadline has passed..."`

**Expected Frontend Behavior:**
- Display error: "The RSVP deadline has passed"
- Disable RSVP buttons
- Show deadline date clearly

---

### Scenario 4: Invalid Guest Count
**Use:** `aaaaaaaa-0001-0001-0001-000000000001`

```graphql
mutation InvalidGuestCount {
  respondToInvitation(input: {
    invitationId: "aaaaaaaa-0001-0001-0001-000000000001"
    status: ACCEPTED
    guestCount: { adults: 0, kids: 0 } # Invalid: total = 0
  }) {
    invitation { id }
  }
}
```

**Expected Error:** `IllegalArgumentException: "At least one guest (adult or kid) is required for RSVP"`

**Expected Frontend Behavior:**
- Form validation should prevent this
- If bypassed, show error: "At least one guest is required"

---

### Scenario 5: Invitation Not Found
**Use:** `inv-999-does-not-exist`

```graphql
query NonExistentInvitation {
  invitation(id: "inv-999-does-not-exist") {
    id
  }
}
```

**Expected Result:** `invitation: null`

**Expected Frontend Behavior:**
- Display "Invitation not found" screen
- Provide link to home page

---

## Frontend Testing Workflow

### 1. Start Backend
```bash
cd hestia-backend
mvn spring-boot:run
# Backend runs at http://localhost:8080
# GraphQL Playground: http://localhost:8080/graphiql
```

### 2. Verify Test Data Loaded
```bash
# Connect to PostgreSQL
psql -U postgres -d hestia_dev

# Verify seed data
SELECT COUNT(*) FROM users WHERE email LIKE '%test.com';
-- Expected: 2

SELECT COUNT(*) FROM events WHERE name LIKE 'TEST:%';
-- Expected: 3

SELECT COUNT(*) FROM invitations WHERE guest_email LIKE '%test.com';
-- Expected: 5

SELECT COUNT(*) FROM rsvps WHERE invitation_id LIKE '%test%';
-- Expected: 3
```

### 3. Generate Frontend Types
```bash
cd hestia-frontend
npm run codegen
# Generates types from http://localhost:8080/graphql
```

### 4. Start Frontend
```bash
npm start
# Web: http://localhost:8081
```

### 5. Test RSVP Flow
1. Navigate to: `http://localhost:8081/invitation/rsvp/aaaaaaaa-0001-0001-0001-000000000001`
2. Verify invitation displays correctly
3. Fill out RSVP form (Accept, 2 adults, 1 kid)
4. Submit RSVP
5. Verify success message and updated status

---

## Database Cleanup

To reset test data for fresh testing:

```sql
-- Delete all test RSVPs
DELETE FROM rsvps WHERE invitation_id IN (
  SELECT id FROM invitations WHERE guest_email LIKE '%test.com'
);

-- Reset invitation statuses to PENDING
UPDATE invitations
SET status = 'PENDING', updated_at = NOW()
WHERE guest_email LIKE '%test.com';
```

Or re-run migration:
```bash
cd hestia-backend
mvn flyway:clean
mvn flyway:migrate
# WARNING: This deletes ALL data and re-seeds
```

---

## API Endpoints

### GraphQL Playground
- **URL:** http://localhost:8080/graphiql
- **Use:** Interactive GraphQL query testing
- **Auth:** None required for test data

### REST Health Check
- **URL:** http://localhost:8080/actuator/health
- **Use:** Verify backend is running

---

## Test Data Summary Table

| Invitation ID | Event | Guest | Status | RSVP | Deadline | Use Case |
|---------------|-------|-------|--------|------|----------|----------|
| `aaaaaaaa-0001-0001-0001-000000000001` | Summer BBQ | Sarah Martinez | PENDING | None | ✅ Future | First-time RSVP |
| `aaaaaaaa-0002-0002-0002-000000000002` | Wedding | Michael Chen | ACCEPTED | Yes (2A, 0K) | ✅ Future | Duplicate error |
| `aaaaaaaa-0003-0003-0003-000000000003` | Birthday | Emily Rodriguez | PENDING | None | ❌ Expired | Deadline error |
| `aaaaaaaa-0004-0004-0004-000000000004` | Summer BBQ | David Lee | DECLINED | Yes (0A, 0K) | ✅ Future | Declined display |
| `aaaaaaaa-0005-0005-0005-000000000005` | Wedding | Jessica Brown | TENTATIVE | Yes (1A, 1K) | ✅ Future | Tentative display |

---

## Notes

- All test data uses `@test.com` email domain for easy identification
- Event names prefixed with `TEST:` for easy filtering
- UUIDs use patterns like `11111111-...` for easy recognition
- Test data is safe to reset/recreate at any time
- **Do NOT run V008 migration in production** (dev/test only)

package com.hestia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for the household calendar (HES-CAL) against a real Postgres (Testcontainers)
 * and the full Spring context. Covers: authed query returns seeded data + backend-computed
 * coverage gap + load (UAC-1/5/6, TAC-2); unauthenticated query is rejected (TAC-1); seed is not
 * duplicated (TAC-4); createCalendarEvent persists + validates (UAC-11, TAC-11).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@Testcontainers
class HouseholdCalendarIT {

    static {
        System.setProperty("aws.region", "us-east-1");
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16").withDatabaseName("hestia");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("hestia.jwt.secret", () -> "test-signing-secret-key-at-least-32-bytes-long");
    }

    @Autowired private HttpGraphQlTester graphQlTester;

    private static final String CAL_QUERY =
            """
            query($p: CalendarPeriodInput!) {
              householdCalendar(period: $p) {
                household { name timezone }
                period { range start end label }
                members { displayName colorHex kind isResponsibleCapable }
                events { title timeLabel colorHex dayOfWeek isCoverageGap responsibleMember { displayName } }
                coverageGaps { shortLabel }
                load { total entries { member { displayName } count percent } }
                connectedAccounts { label statusLabel }
              }
            }
            """;

    private HttpGraphQlTester authed() {
        String email = "cal-" + System.nanoTime() + "@example.com";
        graphQlTester
                .document("mutation($i:RegisterUserInput!){registerUser(input:$i){userId}}")
                .variable(
                        "i",
                        Map.of(
                                "email", email,
                                "password", "password123",
                                "firstName", "Cal",
                                "lastName", "Tester"))
                .execute()
                .path("registerUser.userId")
                .hasValue();
        String token =
                graphQlTester
                        .document("mutation($i:LoginInput!){login(input:$i){accessToken}}")
                        .variable("i", Map.of("email", email, "password", "password123"))
                        .execute()
                        .path("login.accessToken")
                        .entity(String.class)
                        .get();
        return graphQlTester.mutate().headers(h -> h.setBearerAuth(token)).build();
    }

    @Test
    void authedWeekQueryReturnsSeededHouseholdWithComputedFields() {
        authed()
                .document(CAL_QUERY)
                .variable("p", Map.of("anchor", "2026-06-24", "range", "WEEK"))
                .execute()
                .path("householdCalendar.household.name")
                .entity(String.class)
                .isEqualTo("The Hearth")
                .path("householdCalendar.members")
                .entityList(Object.class)
                .hasSize(4)
                .path("householdCalendar.period.start")
                .entity(String.class)
                .isEqualTo("2026-06-22")
                .path("householdCalendar.coverageGaps")
                .entityList(Object.class)
                .satisfies(gaps -> assertThat(gaps).isNotEmpty())
                .path("householdCalendar.load.total")
                .entity(Integer.class)
                .satisfies(total -> assertThat(total).isGreaterThan(0));
    }

    @Test
    void unauthenticatedQueryIsRejected() {
        graphQlTester
                .document(CAL_QUERY)
                .variable("p", Map.of("anchor", "2026-06-24", "range", "WEEK"))
                .execute()
                .errors()
                .satisfy(
                        errors -> {
                            assertThat(errors).isNotEmpty();
                            assertThat(errors.get(0).getMessage()).contains("Authentication required");
                        });
    }

    @Test
    void seedIsNotDuplicated() {
        // Exactly one household + four members proves the idempotent ON CONFLICT seed (TAC-4).
        authed()
                .document(CAL_QUERY)
                .variable("p", Map.of("anchor", "2026-06-24", "range", "WEEK"))
                .execute()
                .path("householdCalendar.members")
                .entityList(Object.class)
                .hasSize(4);
    }

    @Test
    void createCalendarEventPersistsAndAppearsOnItsDay() {
        HttpGraphQlTester user = authed();

        // Maya owner, Pallavi responsible (from seed ids)
        String maya = "00000000-0000-0000-0000-00000000a003";
        String pallavi = "00000000-0000-0000-0000-00000000a001";

        user.document(
                        """
                        mutation($i: CreateCalendarEventInput!) {
                          createCalendarEvent(input: $i) {
                            event { title timeLabel date responsibleMember { displayName } isCoverageGap }
                          }
                        }
                        """)
                .variable(
                        "i",
                        Map.of(
                                "title", "Dentist — Maya",
                                "date", "2026-06-25",
                                "startTime", "15:00",
                                "endTime", "15:45",
                                "allDay", false,
                                "ownerMemberIds", List.of(maya),
                                "responsibleMemberId", pallavi,
                                "needsDriver", true))
                .execute()
                .path("createCalendarEvent.event.title")
                .entity(String.class)
                .isEqualTo("Dentist — Maya")
                .path("createCalendarEvent.event.timeLabel")
                .entity(String.class)
                .isEqualTo("3:00 – 3:45")
                .path("createCalendarEvent.event.responsibleMember.displayName")
                .entity(String.class)
                .isEqualTo("Pallavi");

        // It now shows on that day (2026-06-25 is a Thursday).
        user.document(CAL_QUERY)
                .variable("p", Map.of("anchor", "2026-06-25", "range", "DAY"))
                .execute()
                .path("householdCalendar.events[*].title")
                .entityList(String.class)
                .satisfies(titles -> assertThat(titles).contains("Dentist — Maya"));
    }

    @Test
    void createEventValidationRejectsBlankTitle() {
        List<String> maya = List.of("00000000-0000-0000-0000-00000000a003");
        authed()
                .document(
                        "mutation($i: CreateCalendarEventInput!){createCalendarEvent(input:$i){event{id}}}")
                .variable(
                        "i",
                        Map.of(
                                "title", "   ",
                                "date", "2026-06-25",
                                "startTime", "10:00",
                                "allDay", false,
                                "ownerMemberIds", maya,
                                "needsDriver", false))
                .execute()
                .errors()
                .satisfy(
                        errors -> {
                            assertThat(errors).isNotEmpty();
                            assertThat(errors.get(0).getMessage()).contains("Title is required");
                        });
    }

    @Test
    void malformedAnchorReturnsClassifiedError() {
        authed()
                .document(CAL_QUERY)
                .variable("p", Map.of("anchor", "not-a-date", "range", "WEEK"))
                .execute()
                .errors()
                .satisfy(
                        errors -> {
                            assertThat(errors).isNotEmpty();
                            assertThat(errors.get(0).getMessage()).contains("ISO");
                        });
    }
}

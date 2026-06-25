package com.hestia.event.infrastructure.graphql;

import com.hestia.event.application.CalendarViews.HouseholdCalendarView;
import com.hestia.event.application.HouseholdCalendarService;
import com.hestia.event.application.InvalidEventException;
import com.hestia.event.application.UnauthenticatedException;
import com.hestia.event.domain.CalendarRange;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

/**
 * `householdCalendar` query. Requires an authenticated user (TAC-1).
 *
 * <p>DEMO SCOPING (known limitation, intentional for this slice): there is a single seeded
 * household, resolved via {@code findDefaultHousehold()}. There is NO authUserId→household mapping
 * yet (the {@code household_member.user_id} column is reserved for it). This is safe only while
 * exactly one household exists; before a second household can be created, authorization MUST be
 * scoped to the households the caller is actually a member of. Tracked as a follow-up.
 */
@Controller
public class HouseholdCalendarQueryResolver {

    private final HouseholdCalendarService service;

    public HouseholdCalendarQueryResolver(HouseholdCalendarService service) {
        this.service = service;
    }

    @QueryMapping
    public HouseholdCalendarView householdCalendar(
            @Argument CalendarPeriodInput period,
            @ContextValue(name = "authUserId", required = false) String authUserId) {
        if (authUserId == null) {
            throw new UnauthenticatedException();
        }
        return service.getCalendar(period.anchor(), period.range());
    }

    @GraphQlExceptionHandler
    public GraphQLError handleUnauthenticated(
            UnauthenticatedException ex, DataFetchingEnvironment env) {
        return CalendarErrors.error(ErrorType.UNAUTHORIZED, ex.getMessage(), env);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleInvalidInput(InvalidEventException ex, DataFetchingEnvironment env) {
        return CalendarErrors.error(ErrorType.BAD_REQUEST, ex.getMessage(), env);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleUnavailable(IllegalStateException ex, DataFetchingEnvironment env) {
        // No seeded household (server misconfiguration) — classify instead of opaque INTERNAL_ERROR.
        return CalendarErrors.error(ErrorType.INTERNAL_ERROR, ex.getMessage(), env);
    }

    /** Maps the GraphQL {@code CalendarPeriodInput}. */
    public record CalendarPeriodInput(String anchor, CalendarRange range) {}
}

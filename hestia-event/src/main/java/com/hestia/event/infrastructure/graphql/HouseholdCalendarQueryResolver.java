package com.hestia.event.infrastructure.graphql;

import com.hestia.event.application.CalendarViews.HouseholdCalendarView;
import com.hestia.event.application.HouseholdCalendarService;
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
 * `householdCalendar` query. Requires an authenticated user (TAC-1) — there is no client-supplied
 * household id, so every authed user views the single seeded household with no IDOR surface.
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

    /** Maps the GraphQL {@code CalendarPeriodInput}. */
    public record CalendarPeriodInput(String anchor, CalendarRange range) {}
}

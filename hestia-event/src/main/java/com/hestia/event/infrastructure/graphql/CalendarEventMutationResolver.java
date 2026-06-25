package com.hestia.event.infrastructure.graphql;

import com.hestia.event.application.CalendarViews.EventView;
import com.hestia.event.application.CreateEventCommand;
import com.hestia.event.application.HouseholdCalendarService;
import com.hestia.event.application.InvalidEventException;
import com.hestia.event.application.UnauthenticatedException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

/** `createCalendarEvent` mutation — auth-gated, server-side validated (TAC-11). */
@Controller
public class CalendarEventMutationResolver {

    private final HouseholdCalendarService service;

    public CalendarEventMutationResolver(HouseholdCalendarService service) {
        this.service = service;
    }

    @MutationMapping
    public CreateCalendarEventPayload createCalendarEvent(
            @Argument CreateCalendarEventInput input,
            @ContextValue(name = "authUserId", required = false) String authUserId) {
        if (authUserId == null) {
            throw new UnauthenticatedException();
        }
        LocalDate date;
        try {
            date = LocalDate.parse(input.date());
        } catch (DateTimeParseException ex) {
            throw new InvalidEventException("Date must be ISO yyyy-MM-dd.");
        }
        List<UUID> owners = parseIds(input.ownerMemberIds());
        UUID responsible =
                input.responsibleMemberId() == null
                        ? null
                        : parseId(input.responsibleMemberId(), "responsible adult");
        CreateEventCommand command =
                new CreateEventCommand(
                        input.title(),
                        date,
                        input.startTime(),
                        input.endTime(),
                        input.allDay(),
                        owners,
                        responsible,
                        input.needsDriver(),
                        input.location());
        return new CreateCalendarEventPayload(service.createEvent(command));
    }

    private static List<UUID> parseIds(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().map(id -> parseId(id, "owner")).toList();
    }

    private static UUID parseId(String id, String field) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new InvalidEventException("Invalid " + field + " id.");
        }
    }

    @GraphQlExceptionHandler
    public GraphQLError handleInvalidEvent(InvalidEventException ex, DataFetchingEnvironment env) {
        return CalendarErrors.error(ErrorType.BAD_REQUEST, ex.getMessage(), env);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleUnauthenticated(
            UnauthenticatedException ex, DataFetchingEnvironment env) {
        return CalendarErrors.error(ErrorType.UNAUTHORIZED, ex.getMessage(), env);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleUnavailable(IllegalStateException ex, DataFetchingEnvironment env) {
        return CalendarErrors.error(ErrorType.INTERNAL_ERROR, ex.getMessage(), env);
    }

    /** Maps the GraphQL {@code CreateCalendarEventInput}. */
    public record CreateCalendarEventInput(
            String title,
            String date,
            String startTime,
            String endTime,
            boolean allDay,
            List<String> ownerMemberIds,
            String responsibleMemberId,
            boolean needsDriver,
            String location) {}

    /** Maps the GraphQL {@code CreateCalendarEventPayload}. */
    public record CreateCalendarEventPayload(EventView event) {}
}

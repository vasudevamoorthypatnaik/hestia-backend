package com.hestia.event.infrastructure.graphql;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.springframework.graphql.execution.ErrorType;

/** Builds classified GraphQL errors for the calendar resolvers (mirrors AuthMutationResolver). */
final class CalendarErrors {

    private CalendarErrors() {}

    static GraphQLError error(ErrorType type, String message, DataFetchingEnvironment env) {
        return GraphQLError.newError()
                .errorType(type)
                .message(message)
                .extensions(Map.of("userMessage", message))
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }
}

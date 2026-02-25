package com.example.best_schedule.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;
import graphql.schema.DataFetchingEnvironment;

@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex,
                                                DataFetchingEnvironment env) {

        if (ex instanceof EmailAlreadyExistsException) {
            return GraphqlErrorBuilder.newError(env)
                    .message(ex.getMessage())
                    .build();
        }

        if (ex instanceof DataIntegrityViolationException) {
            return GraphqlErrorBuilder.newError(env)
                    .message("Email already exists")
                    .build();
        }

        if (ex instanceof InvalidCredentialsException) {
            return GraphqlErrorBuilder.newError(env)
                    .message(ex.getMessage())
                    .build();
        }

        return null;
    }
}
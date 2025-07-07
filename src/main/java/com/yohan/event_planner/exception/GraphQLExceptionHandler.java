package com.yohan.event_planner.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles exceptions thrown in GraphQL data fetchers and maps them to GraphQL errors.
 * Produces GraphQL-idiomatic error responses with clean extensions for clients.
 */
@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GraphQLExceptionHandler.class);

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        HttpStatus status = mapHttpStatus(ex);

        String errorCode;
        String message;

        if (ex instanceof EventOwnershipException || ex instanceof UserOwnershipException) {
            errorCode = "USER_OWNERSHIP_VIOLATION";
            message = ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? ex.getMessage()
                    : "You do not have permission to perform this action.";
        } else if (ex instanceof HasErrorCode codeEx) {
            errorCode = codeEx.getErrorCode().name();
            message = ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? ex.getMessage()
                    : "An unexpected error occurred.";
        } else if (ex instanceof BindException bindEx) {
            errorCode = "VALIDATION_ERROR";
            message = bindEx.getFieldErrors().stream()
                    .map(err -> String.format("%s: %s", err.getField(), err.getDefaultMessage()))
                    .collect(Collectors.joining("; "));
            if (message.isBlank()) {
                message = "Input validation failed.";
            }
        } else if (ex instanceof ConstraintViolationException violationEx) {
            boolean durationViolation = violationEx.getConstraintViolations().stream()
                    .anyMatch(v -> v.getPropertyPath().toString().toLowerCase().contains("duration")
                            || v.getMessage().toLowerCase().contains("must be greater than or equal to"));
            boolean urlViolation = violationEx.getConstraintViolations().stream()
                    .anyMatch(v -> v.getPropertyPath().toString().toLowerCase().contains("mediaurl")
                            || v.getMessage().toLowerCase().contains("url"));

            if (durationViolation) {
                errorCode = "INVALID_DURATION";
            } else if (urlViolation) {
                errorCode = "INVALID_URL";
            } else {
                errorCode = "VALIDATION_ERROR";
            }

            message = violationEx.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            if (message.isBlank()) {
                message = "Input validation failed.";
            }
        } else {
            errorCode = "UNKNOWN_ERROR";
            message = ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? ex.getMessage()
                    : "An unexpected error occurred.";
        }

        long timestamp = System.currentTimeMillis();
        logException(ex, status, errorCode);

        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("errorCode", errorCode);
        extensions.put("timeStamp", timestamp);
        extensions.put("status", status.value());

        return GraphqlErrorBuilder.newError(env)
                .message(message)
                .extensions(extensions)
                .build();
    }

    private void logException(Throwable ex, HttpStatus status, String errorCode) {
        if (status.is5xxServerError()) {
            logger.error("GraphQL exception [{}]: {}", errorCode, ex.getMessage(), ex);
        } else {
            logger.warn("GraphQL exception [{}]: {}", errorCode, ex.getMessage());
        }
    }

    private HttpStatus mapHttpStatus(Throwable ex) {
        if (ex instanceof ResourceNotFoundException) return HttpStatus.NOT_FOUND;
        if (ex instanceof UnauthorizedException) return HttpStatus.UNAUTHORIZED;
        if (ex instanceof ConflictException) return HttpStatus.CONFLICT;
        if (ex instanceof UsernameException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof EmailException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof PasswordException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof LabelException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof RoleException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof InvalidTimeException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof InvalidEventStateException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof InvalidMediaTypeException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof EventOwnershipException) return HttpStatus.FORBIDDEN;
        if (ex instanceof UserOwnershipException) return HttpStatus.FORBIDDEN;
        if (ex instanceof SystemManagedEntityException) return HttpStatus.FORBIDDEN;
        if (ex instanceof EventAlreadyConfirmedException) return HttpStatus.CONFLICT;
        if (ex instanceof RecurringEventAlreadyConfirmedException) return HttpStatus.CONFLICT;
        if (ex instanceof InvalidRecurrenceRuleException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof IncompleteBadgeReorderListException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof IncompleteBadgeLabelReorderListException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof IncompleteRecapMediaReorderListException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof BindException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof ConstraintViolationException) return HttpStatus.BAD_REQUEST;

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}

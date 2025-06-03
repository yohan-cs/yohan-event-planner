package com.yohan.event_planner.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

import static com.yohan.event_planner.exception.ErrorCode.INVALID_CREDENTIALS;
import static com.yohan.event_planner.exception.ErrorCode.UNKNOWN_ERROR;


/**
 * Centralized handler for exceptions thrown in REST controllers.
 * Converts exceptions into consistent {@link ErrorResponse} objects,
 * using appropriate HTTP status codes,
 * and extracting error codes from exceptions implementing {@link HasErrorCode}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles {@link InvalidTimeException}, typically thrown when event time validation fails.
     */
    @ExceptionHandler(InvalidTimeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTimeException(InvalidTimeException ex) {
        logger.warn("InvalidTimeException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex);
    }

    /**
     * Handles {@link ConflictException}, for scheduling conflicts such as overlapping events.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex) {
        logger.warn("ConflictException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex);
    }

    /**
     * Handles all username-related exceptions, including duplicates and invalid lengths.
     */
    @ExceptionHandler(UsernameException.class)
    public ResponseEntity<ErrorResponse> handleUsernameException(UsernameException ex) {
        logger.warn("UsernameException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = switch (ex.getErrorCode()) {
            case DUPLICATE_USERNAME -> HttpStatus.CONFLICT;
            case INVALID_USERNAME_LENGTH -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        return buildErrorResponse(status, ex);
    }

    /**
     * Handles all email-related exceptions such as duplicates or invalid format.
     */
    @ExceptionHandler(EmailException.class)
    public ResponseEntity<ErrorResponse> handleEmailException(EmailException ex) {
        logger.warn("EmailException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = switch (ex.getErrorCode()) {
            case DUPLICATE_EMAIL -> HttpStatus.CONFLICT;
            case INVALID_EMAIL_FORMAT -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        return buildErrorResponse(status, ex);
    }


    /**
     * Handles all resource not found exceptions such as {@link ResourceNotFoundException},
     * including subclasses like {@link EventNotFoundException} or {@link UserNotFoundException}.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("ResourceNotFoundException [{}]: {}", (ex instanceof HasErrorCode h ? h.getErrorCode() : "NONE"), ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex);
    }

    /**
     * Handles {@link RoleNotFoundException} specifically.
     */
    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotFoundException(RoleNotFoundException ex) {
        logger.warn("RoleNotFoundException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex);
    }

    /**
     * Handles {@link EventOwnershipException} which occurs when a user
     * attempts to modify or access an event they do not own.
     */
    @ExceptionHandler(EventOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleEventOwnershipException(EventOwnershipException ex) {
        logger.warn("EventOwnershipException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex);
    }

    /**
     * Handles {@link UserOwnershipException} which occurs when a user
     * attempts to access or modify another user they do not own.
     */
    @ExceptionHandler(UserOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleUserOwnershipException(UserOwnershipException ex) {
        logger.warn("UserOwnershipException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex);
    }

    /**
     * Handles all password-related exceptions such as weakness or invalid format.
     */
    @ExceptionHandler(PasswordException.class)
    public ResponseEntity<ErrorResponse> handlePasswordException(PasswordException ex) {
        logger.warn("PasswordException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = switch (ex.getErrorCode()) {
            case WEAK_PASSWORD, INVALID_PASSWORD_LENGTH, NULL_PASSWORD -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        return buildErrorResponse(status, ex);
    }

    /**
     * Handles all role-related exceptions such as duplicates or invalid names.
     */
    @ExceptionHandler(RoleException.class)
    public ResponseEntity<ErrorResponse> handleRoleException(RoleException ex) {
        logger.warn("RoleException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = switch (ex.getErrorCode()) {
            case DUPLICATE_ROLE -> HttpStatus.CONFLICT;
            case INVALID_ROLE_NAME -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        return buildErrorResponse(status, ex);
    }

    /**
     * Handles validation errors from {@code @Valid} annotated method arguments,
     * aggregating all field error messages into a single, semicolon-separated string.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        logger.warn("Validation failed: {}", errorMessages);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, errorMessages, null);
    }

    /**
     * Handles {@link BadCredentialsException}, typically thrown when authentication fails due to incorrect credentials.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        logger.warn("BadCredentialsException: {}", ex.getMessage());
        String message = "Invalid username or password";
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, message, INVALID_CREDENTIALS.name());
    }


    /**
     * Handles {@link UnauthorizedException}, thrown when a user attempts to access or perform an action they are not authorized for.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        logger.warn("UnauthorizedException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex);
    }

    /**
     * Catch-all handler for any unexpected, unhandled exceptions.
     * Logs the full stack trace and returns a generic error message to avoid leaking sensitive details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unhandled exception caught: ", ex);
        String message = "An unexpected error occurred. Please try again later.";
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, UNKNOWN_ERROR.name());
    }


    // --- Helper methods ---

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, Exception ex) {
        String message = ex.getMessage();
        String errorCode = (ex instanceof HasErrorCode codeEx) ? codeEx.getErrorCode().name() : null;
        return buildErrorResponse(status, message, errorCode);
    }


    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, String errorCode) {
        return new ResponseEntity<>(
                new ErrorResponse(status.value(), message, errorCode, System.currentTimeMillis()),
                status
        );
    }
}

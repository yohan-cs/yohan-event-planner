package com.yohan.event_planner.exception;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.stream.Collectors;

import static com.yohan.event_planner.exception.ErrorCode.INVALID_CREDENTIALS;
import static com.yohan.event_planner.exception.ErrorCode.NULL_FIELD_NOT_ALLOWED;
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
     * Handles all email-related exceptions such as duplicates, invalid format, and verification failures.
     */
    @ExceptionHandler(EmailException.class)
    public ResponseEntity<ErrorResponse> handleEmailException(EmailException ex) {
        logger.warn("EmailException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = switch (ex.getErrorCode()) {
            case DUPLICATE_EMAIL -> HttpStatus.CONFLICT;
            case INVALID_EMAIL_FORMAT, INVALID_EMAIL_LENGTH, INVALID_EMAIL_DOMAIN -> HttpStatus.BAD_REQUEST;
            case EMAIL_NOT_VERIFIED -> HttpStatus.UNAUTHORIZED;
            case INVALID_VERIFICATION_TOKEN, EXPIRED_VERIFICATION_TOKEN, USED_VERIFICATION_TOKEN, VERIFICATION_FAILED -> HttpStatus.UNAUTHORIZED;
            case EMAIL_SEND_FAILED, EMAIL_SENDING_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
        return buildErrorResponse(status, ex);
    }


    /**
     * Handles all resource not found exceptions such as {@link ResourceNotFoundException},
     * including subclasses like {@link EventNotFoundException} or {@link UserNotFoundException}.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        logger.warn("ResourceNotFoundException [{}]: {}", (ex instanceof HasErrorCode h ? h.getErrorCode() : "NONE"), ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex, request);
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
     * Handles {@link RecurringEventOwnershipException}, which occurs when a user
     * attempts to access or modify a recurring event they do not own.
     */
    @ExceptionHandler(RecurringEventOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleRecurringEventOwnershipException(RecurringEventOwnershipException ex) {
        logger.warn("RecurringEventOwnershipException [{}]: {}", ex.getErrorCode(), ex.getMessage());
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
     * Handles {@link BadgeOwnershipException} which occurs when a user
     * attempts to access or modify a badge they do not own.
     */
    @ExceptionHandler(BadgeOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleBadgeOwnershipException(BadgeOwnershipException ex, HttpServletRequest request) {
        logger.warn("BadgeOwnershipException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex, request);
    }

    /**
     * Handles {@link LabelOwnershipException} which occurs when a user
     * attempts to access or modify a label they do not own.
     */
    @ExceptionHandler(LabelOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleLabelOwnershipException(LabelOwnershipException ex, HttpServletRequest request) {
        logger.warn("LabelOwnershipException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex, request);
    }

    /**
     * Handles {@link InvalidLabelAssociationException} which occurs when invalid
     * label IDs are provided in request body for badge or other entity associations.
     */
    @ExceptionHandler(InvalidLabelAssociationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLabelAssociationException(InvalidLabelAssociationException ex, HttpServletRequest request) {
        logger.warn("InvalidLabelAssociationException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex, request);
    }

    /**
     * Handles all password-related exceptions such as weakness or invalid format.
     */
    @ExceptionHandler(PasswordException.class)
    public ResponseEntity<ErrorResponse> handlePasswordException(PasswordException ex) {
        logger.warn("PasswordException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = switch (ex.getErrorCode()) {
            case WEAK_PASSWORD, INVALID_PASSWORD_LENGTH -> HttpStatus.BAD_REQUEST;
            case INVALID_RESET_TOKEN, EXPIRED_RESET_TOKEN, USED_RESET_TOKEN -> HttpStatus.UNAUTHORIZED;
            case DUPLICATE_PASSWORD -> HttpStatus.CONFLICT;
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
     * Handles all label-related exceptions such as duplicates.
     */
    @ExceptionHandler(LabelException.class)
    public ResponseEntity<ErrorResponse> handleLabelException(LabelException ex) {
        logger.warn("LabelException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = switch (ex.getErrorCode()) {
            case DUPLICATE_LABEL -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        return buildErrorResponse(status, ex);
    }

    /**
     * Handles validation errors from {@code @Valid} annotated method arguments,
     * aggregating all field error messages into a single, semicolon-separated string.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        logger.warn("Validation failed: {}", errorMessages);
        String path = request != null ? request.getRequestURI() : null;
        return buildErrorResponse(HttpStatus.BAD_REQUEST, errorMessages, null, path);
    }

    /**
     * Handles constraint violations from method parameter validation annotations
     * like {@code @Min}, {@code @Max}, {@code @NotNull}, etc.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        String errorMessages = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining("; "));
        logger.warn("Parameter validation failed: {}", errorMessages);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, errorMessages, "VALIDATION_ERROR");
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
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex);
    }

    /**
     * Handles {@link RateLimitExceededException}, thrown when a client exceeds the configured rate limits.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(RateLimitExceededException ex) {
        logger.warn("RateLimitExceededException for operation '{}': {}/{} attempts, retry in {} seconds", 
                   ex.getOperation(), ex.getCurrentAttempts(), ex.getMaxAttempts(), ex.getRetryAfterSeconds());
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(), 
                ex.getMessage(), 
                ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                System.currentTimeMillis(),
                null
        );
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .header("X-RateLimit-Limit", String.valueOf(ex.getMaxAttempts()))
                .header("X-RateLimit-Remaining", String.valueOf(ex.getMaxAttempts() - ex.getCurrentAttempts()))
                .header("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + ex.getRetryAfterSeconds()))
                .body(errorResponse);
    }

    /**
     * Handles deserialization failures where a field was explicitly set to null
     * but disallowed via {@link com.fasterxml.jackson.annotation.JsonSetter}(nulls = Nulls.FAIL).
     * Currently applies to fields like {@code startTime} in {@code EventUpdateDTO}.
     */
    @ExceptionHandler(MismatchedInputException.class)
    public ResponseEntity<ErrorResponse> handleMismatchedInputException(MismatchedInputException ex) {
        String message = "Invalid input format.";
        String errorCode = NULL_FIELD_NOT_ALLOWED.name();

        if (ex.getPath() != null && !ex.getPath().isEmpty()) {
            String fieldName = ex.getPath().get(0).getFieldName();

            if ("startTime".equals(fieldName)) {
                message = "startTime must not be null if included in the request.";
            }
        }

        logger.warn("MismatchedInputException [{}]: {}", errorCode, message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, errorCode);
    }

    /**
     * Handles {@link MethodArgumentTypeMismatchException}, which occurs when there is a type mismatch
     * between the expected and actual type of a method argument, usually in request parameters or path variables.
     * For example, this is thrown when trying to bind a string value to a parameter that expects a numeric type.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid format for parameter: " + ex.getName();
        String errorCode = "INVALID_PARAMETER_FORMAT";

        logger.warn("MethodArgumentTypeMismatchException [{}]: {}", errorCode, message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, errorCode);
    }

    /**
     * Handles {@link HttpMessageNotReadableException}, which occurs when the HTTP request body
     * cannot be read or parsed, typically due to malformed JSON or invalid date formats.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String message = "Malformed JSON request or invalid data format";
        String errorCode = "INVALID_REQUEST_BODY";

        logger.warn("HttpMessageNotReadableException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, errorCode);
    }

    /**
     * Handles {@link InvalidCalendarParameterException}, which occurs when calendar-related parameters
     * are invalid, such as month values outside the range 1-12.
     */
    @ExceptionHandler(InvalidCalendarParameterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCalendarParameterException(InvalidCalendarParameterException ex) {
        logger.warn("InvalidCalendarParameterException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex);
    }

    /**
     * Handles {@link SystemManagedEntityException}, which occurs when a user
     * attempts to modify or delete a system-managed entity such as the default
     * "Unlabeled" label.
     */
    @ExceptionHandler(SystemManagedEntityException.class)
    public ResponseEntity<ErrorResponse> handleSystemManagedEntityException(SystemManagedEntityException ex) {
        logger.warn("SystemManagedEntityException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex);
    }

    /**
     * Handles {@link EventAlreadyConfirmedException}, which occurs when attempting
     * to confirm an event that is already published (i.e., draft = false).
     */
    @ExceptionHandler(EventAlreadyConfirmedException.class)
    public ResponseEntity<ErrorResponse> handleEventAlreadyConfirmedException(EventAlreadyConfirmedException ex) {
        logger.warn("EventAlreadyConfirmedException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex);
    }

    /**
     * Handles {@link RecurringEventAlreadyConfirmedException}, which occurs when attempting
     * to confirm a recurring event that is already published (i.e., draft = false).
     */
    @ExceptionHandler(RecurringEventAlreadyConfirmedException.class)
    public ResponseEntity<ErrorResponse> handleRecurringEventAlreadyConfirmedException(RecurringEventAlreadyConfirmedException ex) {
        logger.warn("RecurringEventAlreadyConfirmedException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex);
    }

    /**
     * Handles {@link InvalidEventStateException}, which occurs when an event
     * is missing required fields like name, startTime, endTime, or label during confirmation.
     */
    @ExceptionHandler(InvalidEventStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEventStateException(InvalidEventStateException ex) {
        logger.warn("InvalidEventStateException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex);
    }

    /**
     * Handles {@link InvalidRecurrenceRuleException}, which occurs when a recurrence rule
     * is structurally invalid or missing required information (e.g., BYDAY missing for WEEKLY).
     */
    @ExceptionHandler(InvalidRecurrenceRuleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRecurrenceRuleException(InvalidRecurrenceRuleException ex) {
        logger.warn("InvalidRecurrenceRuleException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex);
    }

    /**
     * Handles {@link InvalidSkipDayException}, which occurs when attempting
     * to add or remove past skip days, which is not allowed.
     */
    @ExceptionHandler(InvalidSkipDayException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSkipDayException(InvalidSkipDayException ex) {
        logger.warn("InvalidSkipDayException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex);
    }

    /**
     * Handles {@link IncompleteBadgeReorderListException}, thrown when a badge reorder request
     * does not include all badges owned by the user.
     */
    @ExceptionHandler(IncompleteBadgeReorderListException.class)
    public ResponseEntity<ErrorResponse> handleIncompleteReorderListException(IncompleteBadgeReorderListException ex) {
        logger.warn("IncompleteReorderListException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex);
    }

    /**
     * Handles {@link IncompleteBadgeLabelReorderListException}, thrown when a badge label reorder request
     * does not include all labels associated with the badge or includes invalid label IDs.
     */
    @ExceptionHandler(IncompleteBadgeLabelReorderListException.class)
    public ResponseEntity<ErrorResponse> handleIncompleteBadgeLabelReorderListException(IncompleteBadgeLabelReorderListException ex) {
        logger.warn("IncompleteBadgeLabelReorderListException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex);
    }

    /**
     * Handles {@link IncompleteRecapMediaReorderListException}, thrown when a recap media reorder request
     * does not include all media items for the recap.
     */
    @ExceptionHandler(IncompleteRecapMediaReorderListException.class)
    public ResponseEntity<ErrorResponse> handleIncompleteRecapMediaReorderListException(IncompleteRecapMediaReorderListException ex) {
        logger.warn("IncompleteRecapMediaReorderListException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex);
    }

    /**
     * Handles {@link InvalidMediaTypeException}, thrown when an invalid media type is provided.
     */
    @ExceptionHandler(InvalidMediaTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMediaTypeException(InvalidMediaTypeException ex) {
        logger.warn("InvalidMediaTypeException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex);
    }

    /**
     * Handles {@link EventRecapException}, thrown when there are business rule violations
     * related to event recap operations, such as trying to create a duplicate recap.
     */
    @ExceptionHandler(EventRecapException.class)
    public ResponseEntity<ErrorResponse> handleEventRecapException(EventRecapException ex) {
        logger.warn("EventRecapException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex);
    }

    /**
     * Handles {@link EventRecapAlreadyConfirmedException}, thrown when attempting
     * to confirm an event recap that is already confirmed.
     */
    @ExceptionHandler(EventRecapAlreadyConfirmedException.class)
    public ResponseEntity<ErrorResponse> handleEventRecapAlreadyConfirmedException(EventRecapAlreadyConfirmedException ex) {
        logger.warn("EventRecapAlreadyConfirmedException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex);
    }

    /**
     * Handles {@link HttpRequestMethodNotSupportedException}, which occurs when
     * the HTTP method is not supported for the requested endpoint.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String message = "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint. Supported methods: " + ex.getSupportedHttpMethods();
        logger.warn("HttpRequestMethodNotSupportedException: {} for {} {}", 
                   ex.getMessage(), ex.getMethod(), request.getRequestURI());
        String path = request != null ? request.getRequestURI() : null;
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, message, "METHOD_NOT_ALLOWED", path);
    }

    /**
     * Handles {@link org.springframework.web.HttpMediaTypeNotSupportedException}, which occurs when
     * the content type of the request is not supported by the handler method.
     */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(org.springframework.web.HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        String contentType = ex.getContentType() != null ? ex.getContentType().toString() : "none";
        String requestMethod = request.getMethod();
        String requestPath = request.getRequestURI();
        
        logger.warn("HttpMediaTypeNotSupportedException: {} for content type: {} on {} {}", 
                   ex.getMessage(), contentType, requestMethod, requestPath);
        
        // Only handle this as 415 for POST/PUT/PATCH requests that actually need content
        if ("POST".equalsIgnoreCase(requestMethod) || "PUT".equalsIgnoreCase(requestMethod) || "PATCH".equalsIgnoreCase(requestMethod)) {
            String message = "Unsupported media type: " + contentType + ". Supported types: " + ex.getSupportedMediaTypes();
            return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message, "UNSUPPORTED_MEDIA_TYPE", requestPath);
        }
        
        // For other cases, let the generic handler deal with it
        logger.warn("Unexpected HttpMediaTypeNotSupportedException for {} {}, delegating to generic handler", requestMethod, requestPath);
        return handleGenericException(ex);
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

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, Exception ex, HttpServletRequest request) {
        String message = ex.getMessage();
        String errorCode = (ex instanceof HasErrorCode codeEx) ? codeEx.getErrorCode().name() : null;
        String path = request != null ? request.getRequestURI() : null;
        return buildErrorResponse(status, message, errorCode, path);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, String errorCode) {
        return buildErrorResponse(status, message, errorCode, null);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, String errorCode, String path) {
        return new ResponseEntity<>(
                new ErrorResponse(status.value(), message, errorCode, System.currentTimeMillis(), path),
                status
        );
    }
}

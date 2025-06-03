package com.yohan.event_planner.util;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.auth.RegisterRequestDTO;
import com.yohan.event_planner.security.CustomUserDetails;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

import static com.yohan.event_planner.util.TestConstants.*;

/**
 * Utility class for generating reusable test data objects.
 * <p>
 * This class includes helper methods for creating valid DTOs and domain entities,
 * with or without assigned IDs. It uses reflection where necessary to set entity IDs
 * for unit and integration testing scenarios.
 * </p>
 */
@Component
public class TestUtils {

    // region --- User Factories ---

    /**
     * Creates a valid UserCreateDTO using predefined constants.
     * Intended for general-purpose unit or integration testing.
     */
    public static UserCreateDTO createValidUserCreateDTO() {
        return new UserCreateDTO(
                VALID_USERNAME,
                VALID_PASSWORD,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_TIMEZONE
        );
    }

    /**
     * Creates a static valid registration payload using constants.
     * Use this only when you are not concerned about uniqueness.
     */
    public static RegisterRequestDTO createValidRegisterPayload() {
        return new RegisterRequestDTO(
                VALID_USERNAME,
                VALID_PASSWORD,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_TIMEZONE
        );
    }

    /**
     * Creates a valid registration payload with a unique suffix to prevent
     * username/email conflicts in integration tests.
     *
     * @param suffix a string to append to username and email
     * @return a new RegisterRequestDTO with unique credentials
     */
    public static RegisterRequestDTO createValidRegisterPayload(String suffix) {
        return new RegisterRequestDTO(
                "user" + suffix,
                VALID_PASSWORD,
                "user" + suffix + "@example.com",
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_TIMEZONE
        );
    }

    /**
     * Creates a valid User entity without setting an ID.
     * Useful for scenarios where the persistence layer is expected to assign the ID.
     */
    public static User createUserEntityWithoutId() {
        return new User(
                VALID_USERNAME,
                VALID_PASSWORD,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_TIMEZONE
        );
    }

    /**
     * Creates a valid User entity with a default test ID.
     */
    public static User createUserEntityWithId() {
        return createUserEntityWithId(TestConstants.USER_ID);
    }

    /**
     * Creates a valid User entity and assigns the given ID using reflection.
     *
     * @param id the ID to assign to the User
     * @return a User entity with the specified ID
     */
    public static User createUserEntityWithId(Long id) {
        User user = createUserEntityWithoutId();
        setUserId(user, id);
        return user;
    }

    /**
     * Creates a CustomUserDetails object wrapping a default test user.
     */
    public static CustomUserDetails createCustomUserDetails() {
        return new CustomUserDetails(createUserEntityWithId());
    }

    /**
     * Creates a valid UserResponseDTO using predefined constants.
     */
    public static UserResponseDTO createUserResponseDTO() {
        return new UserResponseDTO(
                VALID_USERNAME,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_TIMEZONE
        );
    }

    /**
     * Uses reflection to manually assign an ID to a User entity for testing purposes.
     */
    private static void setUserId(User user, Long id) {
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to set user ID in test", e);
        }
    }

    // endregion

    // region --- Event Factories ---

    /**
     * Creates a valid EventCreateDTO using predefined constants.
     * Includes all required fields and a non-null description.
     */
    public static EventCreateDTO createValidEventCreateDTO() {
        return new EventCreateDTO(
                VALID_EVENT_TITLE,
                VALID_EVENT_START,
                VALID_EVENT_END,
                VALID_EVENT_DESCRIPTION
        );
    }

    /**
     * Creates a valid Event entity without setting an ID.
     * Requires a non-null User as the event creator.
     *
     * @param creator the User who owns the event
     * @return a new Event instance
     */
    public static Event createEventEntityWithoutId(User creator) {
        return new Event(
                VALID_EVENT_TITLE,
                VALID_EVENT_START,
                VALID_EVENT_END,
                creator
        );
    }

    /**
     * Creates a valid Event entity with a default test ID.
     *
     * @param creator the User who owns the event
     * @return an Event entity with an assigned ID
     */
    public static Event createEventEntityWithId(User creator) {
        return createEventEntityWithId(EVENT_ID, creator);
    }

    /**
     * Creates a valid Event entity with the specified ID.
     * Sets the ID via reflection.
     *
     * @param id the ID to assign
     * @param creator the User who owns the event
     * @return an Event entity with the given ID
     */
    public static Event createEventEntityWithId(Long id, User creator) {
        Event event = createEventEntityWithoutId(creator);
        setEventId(event, id);
        return event;
    }

    /**
     * Creates a valid EventResponseDTO using predefined constants and the default test event ID.
     *
     * @return a fully populated EventResponseDTO for test assertions
     */
    /**
     * Creates a valid EventResponseDTO using predefined constants and the default test event ID.
     *
     * @return a fully populated EventResponseDTO for test assertions
     */
    public static EventResponseDTO createEventResponseDTO() {
        return new EventResponseDTO(
                EVENT_ID,
                VALID_EVENT_TITLE,
                VALID_EVENT_START,
                VALID_EVENT_END,
                null,
                null,
                VALID_EVENT_DESCRIPTION,
                USER_ID,
                VALID_USERNAME,
                VALID_TIMEZONE
        );
    }

    /**
     * Creates a timezone-aware {@link EventResponseDTO} directly from the given {@link Event} entity.
     * <p>
     * This method mirrors the logic used in {@code EventServiceImpl#buildEventResponseDTO} and is
     * intended for use in tests that assert DTO output correctness. It automatically includes
     * start/end timezone fields only if they differ from the creator's timezone.
     * </p>
     *
     * @param event the event to convert to a response DTO
     * @return a fully populated {@link EventResponseDTO} matching service-layer output
     */
    public static EventResponseDTO createEventResponseDTO(Event event) {
        String creatorZone = event.getCreator().getTimezone();
        String startZone = event.getStartTimezone();
        String endZone = event.getEndTimezone();

        String startTimeZone = !creatorZone.equals(startZone) ? startZone : null;
        String endTimeZone = !creatorZone.equals(endZone) ? endZone : null;

        return new EventResponseDTO(
                event.getId(),
                event.getName(),
                event.getStartTime(),
                event.getEndTime(),
                startTimeZone,
                endTimeZone,
                event.getDescription(),
                event.getCreator().getId(),
                event.getCreator().getUsername(),
                creatorZone
        );
    }

    /**
     * Uses reflection to manually assign an ID to an Event entity for testing purposes.
     */
    private static void setEventId(Event event, Long id) {
        try {
            Field idField = Event.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(event, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to set event ID in test", e);
        }
    }

    // endregion
}

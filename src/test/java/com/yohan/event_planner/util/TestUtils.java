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
 *
 * <p>
 * This class includes helper methods for creating valid DTOs and domain entities,
 * with or without assigned IDs. It provides separate methods for timed and untimed
 * (open-ended) event creation to support comprehensive test scenarios.
 * </p>
 */
@Component
public class TestUtils {

    // region --- User Factories ---

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

    public static User createUserEntityWithId() {
        return createUserEntityWithId(USER_ID);
    }

    public static User createUserEntityWithId(Long id) {
        User user = createUserEntityWithoutId();
        setUserId(user, id);
        return user;
    }

    public static CustomUserDetails createCustomUserDetails() {
        return new CustomUserDetails(createUserEntityWithId());
    }

    public static UserResponseDTO createUserResponseDTO() {
        return new UserResponseDTO(
                VALID_USERNAME,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_TIMEZONE
        );
    }

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

    // region --- Event Factories (Untimed/Open-Ended) ---

    /**
     * Creates a valid untimed (open-ended) EventCreateDTO.
     */
    public static EventCreateDTO createUntimedEventCreateDTO() {
        return new EventCreateDTO(
                VALID_EVENT_TITLE,
                VALID_EVENT_START,
                null,
                VALID_EVENT_DESCRIPTION
        );
    }

    /**
     * Creates a valid untimed Event entity without an ID.
     */
    public static Event createUntimedEventEntity(User creator) {
        return new Event(
                VALID_EVENT_TITLE,
                VALID_EVENT_START,
                creator
        );
    }

    /**
     * Creates a valid untimed Event entity with an assigned ID.
     */
    public static Event createUntimedEventEntityWithId(Long id, User creator) {
        Event event = createUntimedEventEntity(creator);
        setEventId(event, id);
        return event;
    }

    // endregion

    // region --- Event Factories (Timed) ---

    /**
     * Creates a valid timed EventCreateDTO with end time.
     */
    public static EventCreateDTO createTimedEventCreateDTO() {
        return new EventCreateDTO(
                VALID_EVENT_TITLE,
                VALID_EVENT_START,
                VALID_EVENT_END,
                VALID_EVENT_DESCRIPTION
        );
    }

    /**
     * Creates a valid timed Event entity with endTime and duration.
     */
    public static Event createTimedEventEntity(User creator) {
        Event event = createUntimedEventEntity(creator);
        event.setEndTime(VALID_EVENT_END);

        long minutes = java.time.Duration.between(
                VALID_EVENT_START.withZoneSameInstant(java.time.ZoneOffset.UTC),
                VALID_EVENT_END.withZoneSameInstant(java.time.ZoneOffset.UTC)
        ).toMinutes();
        event.setDurationMinutes((int) minutes);

        return event;
    }

    /**
     * Creates a valid timed Event entity with an assigned ID.
     */
    public static Event createTimedEventEntityWithId(Long id, User creator) {
        Event event = createTimedEventEntity(creator);
        setEventId(event, id);
        return event;
    }

    // endregion

    // region --- Event Response DTOs ---

    /**
     * Creates a valid EventResponseDTO with timed values.
     */
    public static EventResponseDTO createTimedEventResponseDTO() {
        return new EventResponseDTO(
                EVENT_ID,
                VALID_EVENT_TITLE,
                VALID_EVENT_START,
                VALID_EVENT_END,
                VALID_EVENT_DURATION_MINUTES,
                null,
                null,
                VALID_EVENT_DESCRIPTION,
                USER_ID,
                VALID_USERNAME,
                VALID_TIMEZONE
        );
    }

    /**
     * Creates a valid untimed EventResponseDTO using predefined constants.
     */
    public static EventResponseDTO createUntimedEventResponseDTO() {
        return new EventResponseDTO(
                EVENT_ID,
                VALID_EVENT_TITLE,
                VALID_EVENT_START,
                null,
                null,
                null,
                null,
                VALID_EVENT_DESCRIPTION,
                USER_ID,
                VALID_USERNAME,
                VALID_TIMEZONE
        );
    }

    /**
     * Creates an EventResponseDTO from a given Event entity.
     * Handles both timed and untimed events.
     * For timed events, includes start/end timezones only if they differ from the creator's timezone.
     */
    public static EventResponseDTO createEventResponseDTO(Event event) {
        String creatorZone = event.getCreator().getTimezone();

        String startTimeZone = null;
        String endTimeZone = null;

        if (event.getEndTime() != null) {
            String startZone = event.getStartTimezone();
            String endZone = event.getEndTimezone();
            startTimeZone = !creatorZone.equals(startZone) ? startZone : null;
            endTimeZone = !creatorZone.equals(endZone) ? endZone : null;
        }

        return new EventResponseDTO(
                event.getId(),
                event.getName(),
                event.getStartTime(),
                event.getEndTime(),
                event.getDurationMinutes(),
                startTimeZone,
                endTimeZone,
                event.getDescription(),
                event.getCreator().getId(),
                event.getCreator().getUsername(),
                creatorZone
        );
    }

    // endregion

    private static void setEventId(Event event, Long id) {
        try {
            Field idField = Event.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(event, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to set event ID in test", e);
        }
    }
}

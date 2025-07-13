package com.yohan.event_planner.util;

import com.yohan.event_planner.domain.Badge;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.EventRecap;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.LabelTimeBucket;
import com.yohan.event_planner.domain.RecapMedia;
import com.yohan.event_planner.domain.RecurrenceRuleVO;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.RefreshToken;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.LabelColor;
import com.yohan.event_planner.domain.enums.RecapMediaType;
import com.yohan.event_planner.domain.enums.RecurrenceFrequency;
import com.yohan.event_planner.domain.enums.TimeBucketType;
import com.yohan.event_planner.dto.BadgeCreateDTO;
import com.yohan.event_planner.dto.BadgeUpdateDTO;
import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventRecapCreateDTO;
import com.yohan.event_planner.dto.EventRecapResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.RecapMediaCreateDTO;
import com.yohan.event_planner.dto.RecapMediaResponseDTO;
import com.yohan.event_planner.dto.RecurringEventCreateDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.dto.TimeStatsDTO;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.security.CustomUserDetails;
import com.yohan.event_planner.service.ParsedRecurrenceInput;
import com.yohan.event_planner.service.RecurrenceRuleServiceImpl;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.yohan.event_planner.util.TestConstants.COMPLETED_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.EVENT_ID;
import static com.yohan.event_planner.util.TestConstants.EVENT_RECAP_ID;
import static com.yohan.event_planner.util.TestConstants.FULL_DRAFT_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.FUTURE_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.IMPROMPTU_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.INCOMPLETE_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.PARTIAL_DRAFT_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.UNFILLED_DRAFT_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.USER_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_EMAIL;
import static com.yohan.event_planner.util.TestConstants.VALID_EVENT_DESCRIPTION;
import static com.yohan.event_planner.util.TestConstants.VALID_EVENT_DURATION_MINUTES;
import static com.yohan.event_planner.util.TestConstants.VALID_EVENT_TITLE;
import static com.yohan.event_planner.util.TestConstants.VALID_FIRST_NAME;
import static com.yohan.event_planner.util.TestConstants.VALID_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_LABEL_NAME;
import static com.yohan.event_planner.util.TestConstants.VALID_LAST_NAME;
import static com.yohan.event_planner.util.TestConstants.VALID_MONTHLY_RECURRENCE_RULE;
import static com.yohan.event_planner.util.TestConstants.VALID_PASSWORD;
import static com.yohan.event_planner.util.TestConstants.VALID_RECAP_NAME;
import static com.yohan.event_planner.util.TestConstants.VALID_RECAP_NOTES;
import static com.yohan.event_planner.util.TestConstants.VALID_TIMEZONE;
import static com.yohan.event_planner.util.TestConstants.VALID_USERNAME;
import static com.yohan.event_planner.util.TestConstants.getValidEventEndDate;
import static com.yohan.event_planner.util.TestConstants.getValidEventEndFuture;
import static com.yohan.event_planner.util.TestConstants.getValidEventStartDate;
import static com.yohan.event_planner.util.TestConstants.getValidEventStartFuture;

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

    private static final RecurrenceRuleServiceImpl recurrenceRuleService = new RecurrenceRuleServiceImpl();

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

    public static UserCreateDTO createPartialUserCreateDTO() {
        return new UserCreateDTO(
                null, // username missing
                VALID_PASSWORD,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                null, // last name missing
                VALID_TIMEZONE
        );
    }

    public static UserCreateDTO createValidRegisterPayload() {
        return new UserCreateDTO(
                VALID_USERNAME,
                VALID_PASSWORD,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_TIMEZONE
        );
    }

    public static UserCreateDTO createValidRegisterPayload(String suffix) {
        Objects.requireNonNull(suffix, "suffix must not be null");
        return new UserCreateDTO(
                "user" + suffix,
                VALID_PASSWORD,
                "user" + suffix + "@example.com",
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_TIMEZONE
        );
    }

    public static User createValidUserEntity() {
        return new User(
                VALID_USERNAME,
                VALID_PASSWORD,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_TIMEZONE
        );
    }

    public static User createValidUserEntityWithId() {
        return createValidUserEntityWithId(USER_ID);
    }

    public static User createValidUserEntityWithId(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        User user = createValidUserEntity();
        setUserId(user, id);
        return user;
    }

    /**
     * Creates a test user with a specific username.
     */
    public static User createTestUser(String username) {
        Objects.requireNonNull(username, "username must not be null");
        return new User(
                username,
                VALID_PASSWORD,
                username + "@example.com",
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_TIMEZONE
        );
    }

    /**
     * Creates a user entity marked for deletion, simulating a user within the 30-day grace period.
     */
    public static User createUserPendingDeletion(Clock clock) {
        Objects.requireNonNull(clock);
        User user = createValidUserEntityWithId();
        user.markForDeletion(ZonedDateTime.now(clock));
        return user;
    }

    public static CustomUserDetails createCustomUserDetails() {
        return new CustomUserDetails(createValidUserEntityWithId());
    }

    public static UserResponseDTO createValidUserResponseDTO() {
        return new UserResponseDTO(
                VALID_USERNAME,
                VALID_EMAIL,
                VALID_FIRST_NAME,
                VALID_LAST_NAME,
                VALID_TIMEZONE
        );
    }

    public static void setUserId(User user, Long id) {
        setId(user, id);
    }

// endregion




// region --- Event DTO Factories ---

    /**
     * Creates a fully valid EventCreateDTO for a confirmed scheduled event.
     */
    public static EventCreateDTO createValidScheduledEventCreateDTO(Clock clock) {
        Objects.requireNonNull(clock);
        return new EventCreateDTO(
                VALID_EVENT_TITLE,
                getValidEventStartFuture(clock),
                getValidEventEndFuture(clock),
                VALID_EVENT_DESCRIPTION,
                VALID_LABEL_ID,
                false
        );
    }

    /**
     * Creates a valid EventCreateDTO with start and end times in the future (+1 day).
     */
    public static EventCreateDTO createValidFutureEventCreateDTO(Clock clock) {
        Objects.requireNonNull(clock);
        return new EventCreateDTO(
                VALID_EVENT_TITLE,
                getValidEventStartFuture(clock).plusDays(1),
                getValidEventEndFuture(clock).plusDays(1),
                VALID_EVENT_DESCRIPTION,
                VALID_LABEL_ID,
                false
        );
    }

    /**
     * Creates a partially filled EventCreateDTO for a draft event.
     * Missing endTime and labelId.
     */
    public static EventCreateDTO createPartialEventCreateDTO(Clock clock) {
        Objects.requireNonNull(clock);
        return new EventCreateDTO(
                VALID_EVENT_TITLE,
                getValidEventStartFuture(clock),
                null,  // end time missing
                VALID_EVENT_DESCRIPTION,
                null,  // labelId missing
                true
        );
    }

    /**
     * Creates a minimal EventCreateDTO with only a name field for testing very incomplete drafts.
     */
    public static EventCreateDTO createEmptyEventCreateDTO() {
        return new EventCreateDTO(
                VALID_EVENT_TITLE,
                null,
                null,
                null,
                null,
                true
        );
    }

    /**
     * Creates an EventUpdateDTO to mark the event as completed.
     */
    public static EventUpdateDTO createEventUpdateDTOCompleted() {
        return new EventUpdateDTO(
                null,
                null,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true
        );
    }

    /**
     * Creates an EventUpdateDTO to change the event's start and end time.
     */
    public static EventUpdateDTO createEventUpdateDTOWithTimes(ZonedDateTime start, ZonedDateTime end) {
        Objects.requireNonNull(start, "start time must not be null");
        return new EventUpdateDTO(
                null,
                Optional.ofNullable(start),
                Optional.ofNullable(end),
                Optional.empty(),
                Optional.empty(),
                null
        );
    }

    /**
     * Creates an EventUpdateDTO to update the label.
     */
    public static EventUpdateDTO createEventUpdateDTOWithLabel(Long labelId) {
        return new EventUpdateDTO(
                null,
                null,
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(labelId),
                null
        );
    }

    /**
     * Creates an EventUpdateDTO that will clear the description field.
     */
    public static EventUpdateDTO createEventUpdateDTOUnsetDescription() {
        return new EventUpdateDTO(
                null,
                null,
                Optional.empty(),
                Optional.ofNullable(null), // explicit null to clear description
                Optional.empty(),
                null
        );
    }

    /**
     * Creates an EventResponseDTO from a given Event entity.
     * Dynamically includes or omits timezone fields.
     */
    public static EventResponseDTO createEventResponseDTO(Event event) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(event.getCreator(), "event creator must not be null");

        String creatorZone = event.getCreator().getTimezone();
        String startZone = event.getStartTimezone();
        String endZone = event.getEndTimezone();

        String startTimeZone = !creatorZone.equals(startZone) ? startZone : null;
        String endTimeZone = !creatorZone.equals(endZone) ? endZone : null;

        LabelResponseDTO labelDto = TestUtils.createLabelResponseDTO(event.getLabel());

        return new EventResponseDTO(
                event.getId(),
                event.getName(),
                event.getStartTime(),
                event.getEndTime(),
                event.getDurationMinutes(),
                startTimeZone,
                endTimeZone,
                event.getDescription(),
                event.getCreator().getUsername(),
                event.getCreator().getTimezone(),
                labelDto,
                event.isCompleted(),
                event.isUnconfirmed(),
                event.isImpromptu(),
                false
        );
    }

    public static EventResponseDTO createValidScheduledEventResponseDTO(Clock clock) {
        Objects.requireNonNull(clock);

        Label label = createValidLabelWithId(VALID_LABEL_ID, createValidUserEntityWithId(USER_ID));
        LabelResponseDTO labelDto = createLabelResponseDTO(label);

        return new EventResponseDTO(
                EVENT_ID,
                VALID_EVENT_TITLE,
                getValidEventStartFuture(clock),
                getValidEventEndFuture(clock),
                VALID_EVENT_DURATION_MINUTES,
                null,
                null,
                VALID_EVENT_DESCRIPTION,
                VALID_USERNAME,
                VALID_TIMEZONE,
                labelDto,
                false,
                false,
                false,
                false
        );
    }

    public static EventResponseDTO createValidCompletedEventResponseDTO(Clock clock) {
        Objects.requireNonNull(clock);

        Label label = createValidLabelWithId(VALID_LABEL_ID, createValidUserEntityWithId(USER_ID));
        LabelResponseDTO labelDto = createLabelResponseDTO(label);

        return new EventResponseDTO(
                EVENT_ID,
                VALID_EVENT_TITLE,
                TestConstants.getValidEventStartPast(clock),
                TestConstants.getValidEventEndPast(clock),
                VALID_EVENT_DURATION_MINUTES,
                null,
                null,
                VALID_EVENT_DESCRIPTION,
                VALID_USERNAME,
                VALID_TIMEZONE,
                labelDto,
                true,   // completed flag
                false,
                false,
                false
        );
    }

// endregion

// region --- Event Entity Factories ---

    /**
     * Creates a valid confirmed recurring event (not a draft).
     */
    public static RecurringEvent createValidRecurringEvent(User creator, Clock clock) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        RecurringEvent event = RecurringEvent.createRecurringEvent(
                VALID_EVENT_TITLE,
                getValidEventStartFuture(clock).toLocalTime(),
                getValidEventEndFuture(clock).toLocalTime(),
                getValidEventStartDate(clock),
                getValidEventEndDate(clock),
                VALID_EVENT_DESCRIPTION,
                createValidDailyRecurrenceRuleVO(getValidEventStartDate(clock), getValidEventEndDate(clock)),
                creator,
                false // confirmed
        );
        event.setLabel(createValidLabelWithId(VALID_LABEL_ID, creator));
        return event;
    }

    /**
     * Creates a valid unconfirmed recurring event (draft).
     */
    public static RecurringEvent createUnconfirmedRecurringEvent(User creator, Clock clock) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        RecurringEvent event = RecurringEvent.createUnconfirmedDraftRecurringEvent(
                VALID_EVENT_TITLE,
                getValidEventStartDate(clock),
                getValidEventEndDate(clock),
                creator
        );
        event.setDescription(VALID_EVENT_DESCRIPTION);
        event.setRecurrenceRule(createDraftRecurrenceRuleVO("FREQ=DAILY;INTERVAL=1"));
        event.setLabel(createValidLabelWithId(VALID_LABEL_ID, creator));
        return event;
    }

    /**
     * Creates a valid confirmed recurring event with an assigned ID.
     */
    public static RecurringEvent createValidRecurringEventWithId(User creator, Long id, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");

        RecurringEvent event = createValidRecurringEvent(creator, clock);
        setRecurringEventId(event, id);
        return event;
    }
    /**
     * Sets the ID of a RecurringEvent using reflection.
     */
    public static void setRecurringEventId(RecurringEvent event, Long id) {
        setId(event, id);
    }

    /**
     * Creates a valid confirmed scheduled event (not a draft).
     */
    public static Event createValidScheduledEvent(User creator, Clock clock) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        Event event = Event.createEvent(
                VALID_EVENT_TITLE,
                getValidEventStartFuture(clock),
                getValidEventEndFuture(clock),
                creator
        );
        event.setDescription(VALID_EVENT_DESCRIPTION);
        event.setLabel(createValidLabelWithId(VALID_LABEL_ID, creator));
        return event;
    }

    /**
     * Creates a valid confirmed scheduled event with an assigned ID.
     */
    public static Event createValidScheduledEventWithId(Long id, User creator, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");

        Event event = createValidScheduledEvent(creator, clock);
        setEventId(event, id);
        return event;
    }

    /**
     * Creates a confirmed scheduled event with a start and end time in the future (+1 day).
     */
    public static Event createValidFutureEvent(User creator, Clock clock) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        ZonedDateTime futureStart = getValidEventStartFuture(clock).plusDays(1);
        ZonedDateTime futureEnd = getValidEventEndFuture(clock).plusDays(1);

        Event event = Event.createEvent(VALID_EVENT_TITLE, futureStart, futureEnd, creator);
        event.setDescription(VALID_EVENT_DESCRIPTION);
        event.setLabel(createValidLabelWithId(FUTURE_LABEL_ID, creator));
        return event;
    }

    /**
     * Creates a confirmed event that has ended in the past but is not marked completed.
     */
    public static Event createValidIncompletePastEvent(User creator, Clock clock) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        ZonedDateTime pastStart = TestConstants.getValidEventStartPast(clock);
        ZonedDateTime pastEnd = TestConstants.getValidEventEndPast(clock);

        Event event = Event.createEvent(VALID_EVENT_TITLE, pastStart, pastEnd, creator);
        event.setDescription(VALID_EVENT_DESCRIPTION);
        event.setLabel(createValidLabelWithId(INCOMPLETE_LABEL_ID, creator)); // label defined in TestConstants
        return event;
    }

    /**
     * Creates a valid completed event (ended in the past, marked as completed).
     */
    public static Event createValidCompletedEvent(User creator, Clock clock) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        ZonedDateTime pastStart = TestConstants.getValidEventStartPast(clock);
        ZonedDateTime pastEnd = TestConstants.getValidEventEndPast(clock);

        Event event = Event.createEvent(VALID_EVENT_TITLE, pastStart, pastEnd, creator);
        event.setDescription(VALID_EVENT_DESCRIPTION);
        event.setLabel(createValidLabelWithId(COMPLETED_LABEL_ID, creator)); // label defined in TestConstants
        event.setCompleted(true);
        return event;
    }

    /**
     * Creates a valid completed event with an assigned ID.
     */
    public static Event createValidCompletedEventWithId(Long id, User creator, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");

        Event event = createValidCompletedEvent(creator, clock);
        setEventId(event, id);
        return event;
    }

    /**
     * Creates an empty draft event with all fields missing.
     */
    public static Event createEmptyDraftEvent(User creator) {
        Objects.requireNonNull(creator, "creator must not be null");

        Event event = Event.createUnconfirmedDraft(null, null, null, creator);
        event.setLabel(createValidLabelWithId(UNFILLED_DRAFT_LABEL_ID, creator)); // define in TestConstants
        return event;
    }

    /**
     * Creates a partially filled draft event (e.g. name and start time only).
     */
    public static Event createPartialDraftEvent(User creator, Clock clock) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        Event event = Event.createUnconfirmedDraft(VALID_EVENT_TITLE, getValidEventStartFuture(clock), null, creator);
        event.setDescription(VALID_EVENT_DESCRIPTION);
        event.setLabel(createValidLabelWithId(PARTIAL_DRAFT_LABEL_ID, creator)); // label defined in TestConstants
        return event;
    }

    /**
     * Creates a fully filled draft event (still unconfirmed, but all fields present).
     */
    public static Event createValidFullDraftEvent(User creator, Clock clock) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        Event event = Event.createUnconfirmedDraft(
                VALID_EVENT_TITLE,
                getValidEventStartFuture(clock),
                getValidEventEndFuture(clock),
                creator
        );
        event.setDescription(VALID_EVENT_DESCRIPTION);
        event.setLabel(createValidLabelWithId(FULL_DRAFT_LABEL_ID, creator)); // label defined in TestConstants
        return event;
    }

    /**
     * Creates an impromptu event with a start time but no end time.
     */
    public static Event createValidImpromptuEvent(User creator, Clock clock) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        Event event = Event.createImpromptuEvent(getValidEventStartFuture(clock), creator);
        event.setLabel(createValidLabelWithId(IMPROMPTU_LABEL_ID, creator)); // label defined in TestConstants
        return event;
    }

    /**
     * Creates a valid impromptu event with an assigned ID.
     */
    public static Event createValidImpromptuEventWithId(Long id, User creator, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");

        Event event = createValidImpromptuEvent(creator, clock);
        setEventId(event, id);
        return event;
    }

    public static void setEventId(Event event, Long id) {
        setId(event, id);
    }

    // endregion

    // region --- Recurrence Test Helpers ---

    /**
     * Creates a ParsedRecurrenceInput representing daily recurrence.
     */
    public static ParsedRecurrenceInput createParsedRecurrenceInput() {
        return new ParsedRecurrenceInput(
                RecurrenceFrequency.DAILY,
                null,   // daysOfWeek (not needed for DAILY)
                null    // ordinal (only used for MONTHLY)
        );
    }

    /**
     * Creates a mock EventResponseDTO representing a virtual event occurrence.
     */
    public static EventResponseDTO createVirtualEventResponseDTO(RecurringEvent recurringEvent, LocalDate occurrenceDate, ZoneId zone) {
        Objects.requireNonNull(recurringEvent, "recurringEvent must not be null");
        Objects.requireNonNull(recurringEvent.getRecurrenceRule(), "recurrenceRule must not be null");
        Objects.requireNonNull(recurringEvent.getRecurrenceRule().getParsed(), "parsed recurrenceRule must not be null");
        Objects.requireNonNull(occurrenceDate, "occurrenceDate must not be null");
        Objects.requireNonNull(zone, "zone must not be null");

        LocalTime startTime = recurringEvent.getStartTime();
        LocalTime endTime = recurringEvent.getEndTime();

        ZonedDateTime startUtc = null;
        ZonedDateTime endUtc = null;

        if (startTime != null) {
            startUtc = ZonedDateTime.of(occurrenceDate, startTime, zone).withZoneSameInstant(ZoneId.of("UTC"));
        }

        if (endTime != null) {
            endUtc = ZonedDateTime.of(occurrenceDate, endTime, zone).withZoneSameInstant(ZoneId.of("UTC"));
        }

        Integer durationMinutes = (startUtc != null && endUtc != null)
                ? (int) java.time.Duration.between(startUtc, endUtc).toMinutes()
                : null;

        String startTimeZone = zone.getId();
        String endTimeZone = zone.getId();

        LabelResponseDTO label = createLabelResponseDTO(recurringEvent.getLabel());

        return new EventResponseDTO(
                null,
                recurringEvent.getName(),
                startUtc,
                endUtc,
                durationMinutes,
                startTimeZone,
                endTimeZone,
                recurringEvent.getDescription(),
                recurringEvent.getCreator().getUsername(),
                recurringEvent.getCreator().getTimezone(),
                label,
                false,
                false,
                false,
                true
        );
    }

    // endregion

    // region --- Recurring Event DTO Factories ---

    public static RecurringEventCreateDTO createValidRecurringEventCreateDTO(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");

        return new RecurringEventCreateDTO(
                VALID_EVENT_TITLE,
                getValidEventStartFuture(clock).toLocalTime(),
                getValidEventStartFuture(clock).toLocalTime(),
                getValidEventStartDate(clock),
                getValidEventEndDate(clock),
                VALID_EVENT_DESCRIPTION,
                VALID_LABEL_ID,
                VALID_MONTHLY_RECURRENCE_RULE,
                null,
                false // not draft
        );
    }

    /**
     * Creates a RecurringEventResponseDTO from a given RecurringEvent entity.
     * Dynamically includes or omits timezone fields and formats recurrence summary.
     */
    public static RecurringEventResponseDTO createRecurringEventResponseDTO(RecurringEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(event.getCreator(), "event creator must not be null");

        String recurrenceSummary = event.getRecurrenceRule() != null
                ? event.getRecurrenceRule().getSummary()
                : null;
        LabelResponseDTO labelDto = TestUtils.createLabelResponseDTO(event.getLabel());


        return new RecurringEventResponseDTO(
                event.getId(),
                event.getName(),
                event.getStartTime(),
                event.getEndTime(),
                event.getStartDate(),
                event.getEndDate(),
                event.getDescription(),
                labelDto,
                recurrenceSummary,
                event.getSkipDays(),
                event.getCreator().getUsername(),
                event.isUnconfirmed()
        );
    }

    // endregion

    // region --- LabelResponseDTO Factories ---

    public static LabelResponseDTO createLabelResponseDTO(Label label) {
        if (label == null) {
            return null;
        }
        return new LabelResponseDTO(
                label.getId(),
                label.getName(),
                label.getColor(),
                label.getCreator() != null ? label.getCreator().getUsername() : null
        );
    }

    // endregion

    // region --- Label Factories ---

    /**
     * Creates a valid Label entity with the given ID and creator.
     * Uses default label name.
     */
    public static Label createValidLabelWithId(Long id, User creator) {
        return createValidLabelWithId(id, VALID_LABEL_NAME, creator);
    }

    /**
     * Creates a valid Label entity with full control over name.
     */
    public static Label createValidLabelWithId(Long id, String name, User creator) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "label name must not be null");
        Objects.requireNonNull(creator, "creator must not be null");

        Label label = new Label(name, LabelColor.BLUE, creator);
        setLabelId(label, id);
        return label;
    }

    /**
     * Creates a valid Label entity without setting the ID, allowing DB to generate it.
     */
    public static Label createValidLabel(User creator, String name) {
        Objects.requireNonNull(creator, "creator must not be null");
        Objects.requireNonNull(name, "name must not be null");

        return new Label(name, LabelColor.GRAY, creator);
    }

    /**
     * Sets the provided label as the system-managed "unlabeled" label for the given user.
     * Should only be used in tests.
     */
    public static void setUnlabeledLabel(User user, Label label) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(label, "label must not be null");

        try {
            Field unlabeledField = User.class.getDeclaredField("unlabeled");
            unlabeledField.setAccessible(true);
            unlabeledField.set(user, label);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to set user's unlabeled label in test", e);
        }
    }

    public static void setLabelId(Label label, Long id) {
        setId(label, id);
    }

    // endregion

    // region --- RecurrenceRuleVO Factories ---

    /**
     * Creates a confirmed RecurrenceRuleVO for daily recurrence.
     */
    public static RecurrenceRuleVO createValidDailyRecurrenceRuleVO(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");

        ParsedRecurrenceInput parsed = createParsedDailyRecurrenceInput();
        String summary = recurrenceRuleService.buildSummary(parsed, startDate, endDate);
        return new RecurrenceRuleVO(summary, parsed);
    }

    /**
     * Creates a confirmed RecurrenceRuleVO for weekly recurrence on given days.
     */
    public static RecurrenceRuleVO createValidWeeklyRecurrenceRuleVO(Set<DayOfWeek> daysOfWeek, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");

        ParsedRecurrenceInput parsed = createParsedWeeklyRecurrenceInput(daysOfWeek);
        String summary = recurrenceRuleService.buildSummary(parsed,
                getValidEventStartDate(clock),
                getValidEventEndDate(clock));
        return new RecurrenceRuleVO(summary, parsed);
    }

    /**
     * Creates a confirmed RecurrenceRuleVO for monthly recurrence using ordinal and daysOfWeek.
     */
    public static RecurrenceRuleVO createValidMonthlyRecurrenceRuleVO(int ordinal, Set<DayOfWeek> daysOfWeek, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");

        ParsedRecurrenceInput parsed = createParsedMonthlyRecurrenceInput(ordinal, daysOfWeek);
        String summary = recurrenceRuleService.buildSummary(parsed,
                getValidEventStartDate(clock),
                getValidEventEndDate(clock));
        return new RecurrenceRuleVO(summary, parsed);
    }

    /**
     * Creates a draft (unparsed) RecurrenceRuleVO using raw rule string.
     */
    public static RecurrenceRuleVO createDraftRecurrenceRuleVO(String rawRule) {
        return new RecurrenceRuleVO(rawRule, null);
    }

    /**
     * Returns a ParsedRecurrenceInput for daily recurrence.
     * All days are set to true to match parser behavior.
     */
    public static ParsedRecurrenceInput createParsedDailyRecurrenceInput() {
        Set<DayOfWeek> allDays = EnumSet.allOf(DayOfWeek.class);
        return new ParsedRecurrenceInput(RecurrenceFrequency.DAILY, allDays, null);
    }

    /**
     * Returns a ParsedRecurrenceInput for weekly recurrence with given days of the week.
     * daysOfWeek[0] = Sunday, [1] = Monday, ..., [6] = Saturday
     */
    public static ParsedRecurrenceInput createParsedWeeklyRecurrenceInput(Set<DayOfWeek> daysOfWeek) {
        return new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, daysOfWeek, null);
    }

    /**
     * Returns a ParsedRecurrenceInput for monthly recurrence with given ordinal and days of the week.
     */
    public static ParsedRecurrenceInput createParsedMonthlyRecurrenceInput(int ordinal, Set<DayOfWeek> daysOfWeek) {
        return new ParsedRecurrenceInput(RecurrenceFrequency.MONTHLY, daysOfWeek, ordinal);
    }

    // endregion

    // region --- Badge DTO Factories ---

    public static BadgeCreateDTO createValidBadgeCreateDTO() {
        return new BadgeCreateDTO("Consistency", Set.of(VALID_LABEL_ID));
    }

    public static BadgeUpdateDTO createBadgeUpdateDTOWithAllChanges() {
        return new BadgeUpdateDTO(
                "Updated Name"
        );
    }

    public static BadgeUpdateDTO createBadgeUpdateDTORenameOnly(String newName) {
        return new BadgeUpdateDTO(newName);
    }

    // endregion


    // region --- Badge Factories ---

    /**
     * Creates a valid Badge with only label IDs.
     */
    public static Badge createValidBadgeWithLabelIds(User user, Set<Long> labelIds) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(labelIds, "labelIds must not be null");

        Badge badge = createEmptyBadge(user, "Badge with Labels");
        badge.addLabelIds(labelIds);
        return badge;
    }

    /**
     * Creates a valid Badge with a given ID and owner. The badge has no labelsby default.
     */
    public static Badge createValidBadgeWithIdAndOwner(Long id, User user) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(user, "user must not be null");

        Badge badge = createEmptyBadge(user, "Badge with ID");
        setBadgeId(badge, id);
        return badge;
    }

    /**
     * Creates an empty Badge with a custom name.
     */
    public static Badge createEmptyBadge(User user, String name) {
        return new Badge(name, user, 0);
    }

    public static void setBadgeId(Badge badge, Long id) {
        setId(badge, id);
    }

    /**
     * Creates a TimeStatsDTO with all values set to 0.
     */
    public static TimeStatsDTO createEmptyTimeStatsDTO() {
        return new TimeStatsDTO(0, 0, 0, 0, 0, 0);
    }

    // endregion

    // region --- RecapMedia DTO Factory ---

    /**
     * Creates a valid RecapMediaCreateDTO for images.
     */
    public static RecapMediaCreateDTO createValidImageRecapMediaCreateDTO() {
        return new RecapMediaCreateDTO(
                "https://example.com/image.jpg",
                RecapMediaType.IMAGE,
                null, // durationSeconds not needed for image
                0 // default order
        );
    }

    /**
     * Creates a valid RecapMediaCreateDTO for videos.
     */
    public static RecapMediaCreateDTO createValidVideoRecapMediaCreateDTO() {
        return new RecapMediaCreateDTO(
                "https://example.com/video.mp4",
                RecapMediaType.VIDEO,
                30, // e.g. 30 seconds duration
                1 // example order
        );
    }

    /**
     * Creates a valid RecapMediaResponseDTO for images.
     */
    public static RecapMediaResponseDTO createValidImageRecapMediaResponseDTO() {
        return new RecapMediaResponseDTO(
                1L,
                "https://example.com/image.jpg",
                RecapMediaType.IMAGE,
                null,
                0
        );
    }

    /**
     * Creates a valid RecapMediaResponseDTO for videos.
     */
    public static RecapMediaResponseDTO createValidVideoRecapMediaResponseDTO() {
        return new RecapMediaResponseDTO(
                2L,
                "https://example.com/video.mp4",
                RecapMediaType.VIDEO,
                30,
                1
        );
    }

    //

    // region --- RecapMedia Factory ---

    /**
     * Creates a valid RecapMedia entity for an image with the given recap.
     */
    public static RecapMedia createValidImageRecapMedia(EventRecap recap) {
        return new RecapMedia(
                recap,
                "https://example.com/image.jpg",
                RecapMediaType.IMAGE,
                null,
                0
        );
    }

    /**
     * Creates a valid RecapMedia entity for a video with the given recap.
     */
    public static RecapMedia createValidVideoRecapMedia(EventRecap recap) {
        return new RecapMedia(
                recap,
                "https://example.com/video.mp4",
                RecapMediaType.VIDEO,
                30,
                1
        );
    }

    /**
     * Creates a valid RecapMedia entity with ID for images.
     */
    public static RecapMedia createValidImageRecapMediaWithId(EventRecap recap, Long id) {
        RecapMedia media = createValidImageRecapMedia(recap);
        setId(media, id);
        return media;
    }

    /**
     * Creates a valid RecapMedia entity for images with assigned ID.
     */
    public static RecapMedia createValidImageRecapMediaWithId(Long id) {
        Event event = createValidCompletedEventWithId(EVENT_ID, createValidUserEntityWithId(), Clock.systemUTC());
        EventRecap recap = createValidEventRecap(event);

        RecapMedia media = new RecapMedia(
                recap,
                "https://example.com/image.jpg",
                RecapMediaType.IMAGE,
                null,
                0
        );
        setId(media, id);
        return media;
    }

    /**
     * Creates a valid RecapMedia entity with ID for videos.
     */
    public static RecapMedia createValidVideoRecapMediaWithId(EventRecap recap, Long id) {
        RecapMedia media = createValidVideoRecapMedia(recap);
        setId(media, id);
        return media;
    }

    // endregion

    // region --- EventRecap DTO Factory ---

    /**
     * Creates an EventRecapResponseDTO from the given recap and event.
     */
    public static EventRecapResponseDTO createEventRecapResponseDTO(EventRecap recap, Event event) {
        Objects.requireNonNull(recap, "recap must not be null");
        Objects.requireNonNull(event, "event must not be null");

        return new EventRecapResponseDTO(
                recap.getId(),
                event.getName(),
                event.getCreator().getUsername(),
                event.getStartTime(),
                event.getDurationMinutes(),
                event.getLabel() != null ? event.getLabel().getName() : null,
                recap.getNotes(),
                List.of(
                        createValidImageRecapMediaResponseDTO(),
                        createValidVideoRecapMediaResponseDTO()
                ),
                false
        );
    }

    /**
     * Creates a valid confirmed EventRecapCreateDTO with sample media.
     */
    public static EventRecapCreateDTO createValidConfirmedRecapCreateDTO() {
        return new EventRecapCreateDTO(
                EVENT_ID,
                VALID_RECAP_NOTES,
                VALID_RECAP_NAME,
                false, // confirmed
                List.of(createValidImageRecapMediaCreateDTO(), createValidVideoRecapMediaCreateDTO())
        );
    }

    /**
     * Creates a valid unconfirmed (draft) EventRecapCreateDTO with sample media.
     */
    public static EventRecapCreateDTO createValidUnconfirmedRecapCreateDTO() {
        return new EventRecapCreateDTO(
                EVENT_ID,
                VALID_RECAP_NOTES,
                VALID_RECAP_NAME,
                true, // draft
                List.of(createValidImageRecapMediaCreateDTO())
        );
    }

    /**
     * Creates a muted confirmed EventRecapCreateDTO (null notes) with sample media.
     */
    public static EventRecapCreateDTO createMutedConfirmedRecapCreateDTO() {
        return new EventRecapCreateDTO(
                EVENT_ID,
                null, // muted
                VALID_RECAP_NAME,
                false,
                List.of(createValidImageRecapMediaCreateDTO())
        );
    }

    /**
     * Creates a muted unconfirmed EventRecapCreateDTO (null notes) with sample media.
     */
    public static EventRecapCreateDTO createMutedUnconfirmedRecapCreateDTO() {
        return new EventRecapCreateDTO(
                EVENT_ID,
                null,
                VALID_RECAP_NAME,
                true,
                List.of(createValidVideoRecapMediaCreateDTO())
        );
    }

// endregion


    // region --- EventRecap Factory ---

    /**
     * Creates a valid confirmed EventRecap entity for the given completed event.
     */
    public static EventRecap createValidEventRecap(Event event) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(event.getCreator(), "event must have a creator");

        EventRecap recap = EventRecap.createConfirmedRecap(event, event.getCreator(), VALID_RECAP_NOTES, VALID_RECAP_NAME);
        setId(recap, EVENT_RECAP_ID);
        return recap;
    }

    /**
     * Creates a valid unconfirmed (draft) EventRecap entity for the given event.
     */
    public static EventRecap createValidUnconfirmedEventRecap(Event event) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(event.getCreator(), "event must have a creator");

        EventRecap recap = EventRecap.createUnconfirmedRecap(event, event.getCreator(), VALID_RECAP_NOTES, VALID_RECAP_NAME);
        setId(recap, EVENT_RECAP_ID);
        return recap;
    }

    // endregion

// region --- LabelTimeBucket Factories ---

    /**
     * Creates a valid DAY-type LabelTimeBucket.
     */
    public static LabelTimeBucket createValidDayBucket(Long userId, Long labelId, String labelName, int year, int dayOfYear, int minutes) {
        return createValidBucket(userId, labelId, labelName, TimeBucketType.DAY, year, dayOfYear, minutes);
    }

    /**
     * Creates a valid WEEK-type LabelTimeBucket.
     */
    public static LabelTimeBucket createValidWeekBucket(Long userId, Long labelId, String labelName, int year, int weekOfYear, int minutes) {
        return createValidBucket(userId, labelId, labelName, TimeBucketType.WEEK, year, weekOfYear, minutes);
    }

    /**
     * Creates a valid MONTH-type LabelTimeBucket.
     */
    public static LabelTimeBucket createValidMonthBucket(Long userId, Long labelId, String labelName, int year, int monthValue, int minutes) {
        return createValidBucket(userId, labelId, labelName, TimeBucketType.MONTH, year, monthValue, minutes);
    }

    /**
     * Internal reusable bucket creation method.
     */
    private static LabelTimeBucket createValidBucket(Long userId, Long labelId, String labelName,
                                                     TimeBucketType type, int year, int bucketValue, int minutes) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(labelId, "labelId must not be null");
        Objects.requireNonNull(labelName, "labelName must not be null");
        Objects.requireNonNull(type, "type must not be null");

        LabelTimeBucket bucket = new LabelTimeBucket(userId, labelId, labelName, type, year, bucketValue);
        bucket.setDurationMinutes(minutes);
        return bucket;
    }

    // endregion

    // region RefreshToken Test Helpers

    /**
     * Creates a valid RefreshToken entity for testing.
     */
    public static RefreshToken createValidRefreshToken(Long userId) {
        // Use real time + 1 hour to ensure token is valid during test execution
        return createValidRefreshToken(userId, Clock.systemUTC());
    }

    /**
     * Creates a valid RefreshToken entity for testing with the given clock.
     */
    public static RefreshToken createValidRefreshToken(Long userId, Clock clock) {
        RefreshToken token = new RefreshToken();
        setId(token, 1L);
        token.setUserId(userId);
        token.setTokenHash("hashed-token-" + userId);
        token.setExpiryDate(clock.instant().plusSeconds(3600)); // 1 hour from clock time
        token.setRevoked(false);
        return token;
    }

    /**
     * Creates an expired RefreshToken entity for testing.
     */
    public static RefreshToken createExpiredRefreshToken(Long userId) {
        // Use real time - 1 hour to ensure token is expired during test execution
        return createExpiredRefreshToken(userId, Clock.systemUTC());
    }

    /**
     * Creates an expired RefreshToken entity for testing with the given clock.
     */
    public static RefreshToken createExpiredRefreshToken(Long userId, Clock clock) {
        RefreshToken token = new RefreshToken();
        setId(token, 2L);
        token.setUserId(userId);
        token.setTokenHash("hashed-expired-token-" + userId);
        token.setExpiryDate(clock.instant().minusSeconds(3600)); // 1 hour before clock time
        token.setRevoked(false);
        return token;
    }

    /**
     * Creates a revoked RefreshToken entity for testing.
     */
    public static RefreshToken createRevokedRefreshToken(Long userId) {
        // Use real time + 1 hour for expiry, but mark as revoked
        return createRevokedRefreshToken(userId, Clock.systemUTC());
    }

    /**
     * Creates a revoked RefreshToken entity for testing with the given clock.
     */
    public static RefreshToken createRevokedRefreshToken(Long userId, Clock clock) {
        RefreshToken token = new RefreshToken();
        setId(token, 3L);
        token.setUserId(userId);
        token.setTokenHash("hashed-revoked-token-" + userId);
        token.setExpiryDate(clock.instant().plusSeconds(3600)); // 1 hour from clock time
        token.setRevoked(true);
        return token;
    }

    /**
     * Creates a RefreshToken entity with custom expiry for testing.
     */
    public static RefreshToken createRefreshTokenWithExpiry(Long userId, Instant expiryDate) {
        RefreshToken token = new RefreshToken();
        setId(token, 4L);
        token.setUserId(userId);
        token.setTokenHash("hashed-custom-token-" + userId);
        token.setExpiryDate(expiryDate);
        token.setRevoked(false);
        return token;
    }

    // endregion


    /**
     * Uses reflection to set the ID field on any entity that has a field named "id".
     * This method is intended for internal use only inside TestUtils.
     */
    private static void setId(Object entity, Long id) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(id, "id must not be null");
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to set ID for " + entity.getClass().getSimpleName(), e);
        }
    }
}

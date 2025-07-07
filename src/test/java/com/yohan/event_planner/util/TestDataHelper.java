package com.yohan.event_planner.util;

import com.yohan.event_planner.domain.Badge;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.EventRecap;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.LabelTimeBucket;
import com.yohan.event_planner.domain.RecapMedia;
import com.yohan.event_planner.domain.RecurrenceRuleVO;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.RecapMediaType;
import com.yohan.event_planner.domain.enums.TimeBucketType;
import com.yohan.event_planner.repository.BadgeRepository;
import com.yohan.event_planner.repository.EventRecapRepository;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.repository.LabelRepository;
import com.yohan.event_planner.repository.LabelTimeBucketRepository;
import com.yohan.event_planner.repository.RecapMediaRepository;
import com.yohan.event_planner.repository.RecurringEventRepository;
import com.yohan.event_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class TestDataHelper {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final LabelRepository labelRepository;
    private final BadgeRepository badgeRepository;
    private final RecurringEventRepository recurringEventRepository;
    private final EventRecapRepository eventRecapRepository;
    private final RecapMediaRepository recapMediaRepository;
    private final LabelTimeBucketRepository labelTimeBucketRepository;
    private final TestAuthUtils testAuthUtils;
    private final Clock clock;

    @Autowired
    public TestDataHelper(
            UserRepository userRepository,
            EventRepository eventRepository,
            LabelRepository labelRepository,
            BadgeRepository badgeRepository,
            RecurringEventRepository recurringEventRepository,
            EventRecapRepository eventRecapRepository,
            RecapMediaRepository recapMediaRepository,
            LabelTimeBucketRepository labelTimeBucketRepository,
            TestAuthUtils testAuthUtils,
            Clock clock)
    {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.labelRepository = labelRepository;
        this.badgeRepository = badgeRepository;
        this.recurringEventRepository = recurringEventRepository;
        this.eventRecapRepository = eventRecapRepository;
        this.recapMediaRepository = recapMediaRepository;
        this.labelTimeBucketRepository = labelTimeBucketRepository;
        this.testAuthUtils = testAuthUtils;
        this.clock = clock;
    }

    // Helper method to register and login a user
    public TestAuthUtils.AuthResult registerAndLoginUserWithUser(String suffix) throws Exception {
        String jwt = testAuthUtils.registerAndLoginUser(suffix);
        User user = userRepository.findByUsername("user" + suffix)
                .orElseThrow(() -> new IllegalStateException("User not found after registration"));
        return new TestAuthUtils.AuthResult(jwt, user);
    }

    // Helper method to create and persist a label
    public Label createAndPersistLabel(User user, String name) {
        String uniqueName = name + "_" + System.currentTimeMillis();
        var label = TestUtils.createValidLabel(user, uniqueName);
        return labelRepository.saveAndFlush(label);
    }

    // Helper method to create and persist a badge
    public Badge createAndPersistBadge(User user, String name) {
        var badge = TestUtils.createEmptyBadge(user, name);
        return badgeRepository.saveAndFlush(badge);
    }

    // Helper method to create and persist a scheduled event
    public Event createAndPersistScheduledEvent(User user, String name) {
        var label = createAndPersistLabel(user, "Test Label");
        var event = TestUtils.createValidScheduledEvent(user, clock);
        event.setName(name);
        event.setLabel(label);
        return eventRepository.saveAndFlush(event);
    }

    // Helper method to create and persist a future event
    public Event createAndPersistFutureEvent(User user) {
        var label = createAndPersistLabel(user, "Future Label");
        var event = TestUtils.createValidFutureEvent(user, clock);
        event.setLabel(label);
        return eventRepository.saveAndFlush(event);
    }

    // Helper method to create and persist a completed event
    public Event createAndPersistCompletedEvent(User user) {
        var label = createAndPersistLabel(user, "Completed Label");
        var event = TestUtils.createValidCompletedEvent(user, clock);
        event.setLabel(label);
        return eventRepository.saveAndFlush(event);
    }

    // Helper method to create and persist a recurring event
    public RecurringEvent createAndPersistRecurringEvent(User user, String name) {
        var label = createAndPersistLabel(user, "Recurring Label");
        var recurringEvent = TestUtils.createValidRecurringEvent(user, clock);
        recurringEvent.setName(name);
        recurringEvent.setLabel(label);
        return recurringEventRepository.saveAndFlush(recurringEvent);
    }

    // Helper method to create and persist an unconfirmed recurring event
    public RecurringEvent createAndPersistUnconfirmedRecurringEvent(User user, String name) {
        var label = createAndPersistLabel(user, "Recurring Label");
        var recurringEvent = TestUtils.createValidRecurringEvent(user, clock);
        recurringEvent.setName(name);
        recurringEvent.setLabel(label);
        recurringEvent.setUnconfirmed(true);  // Set the confirmation status
        return recurringEventRepository.saveAndFlush(recurringEvent);
    }

    // Helper method to create and persist a confirmed recurring event (simplified)
    public RecurringEvent createAndPersistConfirmedRecurringEvent(User user) {
        return createAndPersistRecurringEvent(user, "Test Recurring Event");
    }

    // Helper method to create and persist a past recurring event
    public RecurringEvent createAndPersistPastRecurringEvent(User user) {
        var label = createAndPersistLabel(user, "Past Recurring Label");
        var recurringEvent = TestUtils.createValidRecurringEvent(user, clock);
        
        // Set dates in the past (relative to fixed test clock: 2025-06-27)
        recurringEvent.setStartDate(TestConstants.getFixedTodayUserZone(clock).minusDays(60)); // 2025-04-28
        recurringEvent.setEndDate(TestConstants.getFixedTodayUserZone(clock).minusDays(30));   // 2025-05-28
        
        recurringEvent.setName("Past Recurring Event");
        recurringEvent.setLabel(label);
        return recurringEventRepository.saveAndFlush(recurringEvent);
    }

    // Helper method to create and persist a future recurring event
    public RecurringEvent createAndPersistFutureRecurringEvent(User user) {
        var label = createAndPersistLabel(user, "Future Recurring Label");
        var recurringEvent = TestUtils.createValidRecurringEvent(user, clock);
        
        // Set dates in the future (relative to fixed test clock: 2025-06-27)
        recurringEvent.setStartDate(TestConstants.getFixedTodayUserZone(clock).plusDays(10)); // 2025-07-07
        recurringEvent.setEndDate(TestConstants.getFixedTodayUserZone(clock).plusDays(40));   // 2025-08-06
        
        recurringEvent.setName("Future Recurring Event");
        recurringEvent.setLabel(label);
        return recurringEventRepository.saveAndFlush(recurringEvent);
    }

    // Helper method to create and persist a completed event with recap
    public Event createAndPersistCompletedEventWithRecap(User user, String eventName, String notes) {
        var event = createAndPersistCompletedEvent(user);
        event.setName(eventName);

        var recap = EventRecap.createConfirmedRecap(event, user, notes, eventName + " Recap");
        event.setRecap(recap);

        return eventRepository.saveAndFlush(event);
    }

    // Helper method to create and persist a completed event with an unconfirmed recap
    public Event createAndPersistCompletedEventWithUnconfirmedRecap(User user, String eventName) {
        var event = createAndPersistCompletedEvent(user);
        event.setName(eventName);

        var recap = EventRecap.createUnconfirmedRecap(event, user, "Draft recap notes", eventName + " Draft Recap");
        event.setRecap(recap);

        return eventRepository.saveAndFlush(event);
    }

    // Helper method to create and persist RecapMedia
    public RecapMedia createAndPersistRecapMedia(EventRecap recap, String mediaUrl, RecapMediaType mediaType, Integer durationSeconds, Integer mediaOrder) {
        var media = new RecapMedia(
                recap,
                mediaUrl != null ? mediaUrl : "https://example.com/default.jpg",
                mediaType != null ? mediaType : RecapMediaType.IMAGE,
                durationSeconds != null ? durationSeconds : 0,
                mediaOrder != null ? mediaOrder : 0
        );

        recap.getMedia().add(media);
        return recapMediaRepository.saveAndFlush(media);
    }

    // Helper method to create and persist LabelTimeBucket
    public LabelTimeBucket createAndPersistLabelTimeBucket(User user, Label label, TimeBucketType bucketType, int year, int bucketValue, int durationMinutes) {
        // Use your domain constructor for correct initialization
        LabelTimeBucket bucket = new LabelTimeBucket(
                user.getId(),
                label.getId(),
                label.getName(),
                bucketType,
                year,
                bucketValue
        );
        bucket.setDurationMinutes(durationMinutes);
        return labelTimeBucketRepository.saveAndFlush(bucket);
    }

    public void saveAndFlush(Object entity) {
        if (entity instanceof Badge) {
            badgeRepository.saveAndFlush((Badge) entity);
        } else if (entity instanceof User) {
            userRepository.saveAndFlush((User) entity);
        } else if (entity instanceof Event) {
            eventRepository.saveAndFlush((Event) entity);
        } else if (entity instanceof RecurringEvent) {
            recurringEventRepository.saveAndFlush((RecurringEvent) entity);
        } else if (entity instanceof Label) {
            labelRepository.saveAndFlush((Label) entity);
        } else if (entity instanceof EventRecap) {
            eventRecapRepository.saveAndFlush((EventRecap) entity);
        } else if (entity instanceof RecapMedia) {
            recapMediaRepository.saveAndFlush((RecapMedia) entity);
        } else if (entity instanceof LabelTimeBucket) {
            labelTimeBucketRepository.saveAndFlush((LabelTimeBucket) entity);
        } else {
            throw new IllegalArgumentException("Unsupported entity type: " + entity.getClass());
        }
    }
}
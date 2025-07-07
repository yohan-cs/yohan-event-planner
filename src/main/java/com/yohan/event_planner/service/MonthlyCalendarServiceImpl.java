package com.yohan.event_planner.service;

import com.yohan.event_planner.business.RecurringEventBO;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.LabelTimeBucket;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.TimeBucketType;
import com.yohan.event_planner.dto.LabelMonthStatsDTO;
import com.yohan.event_planner.exception.LabelNotFoundException;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.repository.LabelRepository;
import com.yohan.event_planner.repository.LabelTimeBucketRepository;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.yohan.event_planner.domain.enums.TimeBucketType.MONTH;

@Service
public class MonthlyCalendarServiceImpl implements MonthlyCalendarService{

    private final RecurringEventBO recurringEventBO;
    private final RecurrenceRuleService recurrenceRuleService;
    private final LabelTimeBucketRepository labelTimeBucketRepository;
    private final EventRepository eventRepository;
    private final LabelRepository labelRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final OwnershipValidator ownershipValidator;

    public MonthlyCalendarServiceImpl(
            RecurringEventBO recurringEventBO,
            RecurrenceRuleService recurrenceRuleService,
            LabelTimeBucketRepository labelTimeBucketRepository,
            EventRepository eventRepository,
            LabelRepository labelRepository,
            AuthenticatedUserProvider authenticatedUserProvider,
            OwnershipValidator ownershipValidator
    ) {
        this.recurringEventBO = recurringEventBO;
        this.recurrenceRuleService = recurrenceRuleService;
        this.labelTimeBucketRepository = labelTimeBucketRepository;
        this.eventRepository = eventRepository;
        this.labelRepository = labelRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.ownershipValidator = ownershipValidator;
    }

    @Override
    public LabelMonthStatsDTO getMonthlyBucketStats(Long labelId, int year, int month) {
        // Fetch the user's time zone
        User viewer = authenticatedUserProvider.getCurrentUser();
        ZoneId userZoneId = ZoneId.of(viewer.getTimezone());

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new LabelNotFoundException(labelId));

        ownershipValidator.validateLabelOwnership(viewer.getId(), label);

        // Adjust start and end of the month based on user's time zone
        ZonedDateTime startOfMonthUserTimeZone = LocalDate.of(year, month, 1).atStartOfDay(userZoneId);
        LocalDate lastDayOfMonth = LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());
        ZonedDateTime endOfMonthUserTimeZone = lastDayOfMonth.atTime(23, 59).atZone(userZoneId);

        // Convert the start and end of the month to UTC for the database query
        ZonedDateTime startOfMonthUtc = startOfMonthUserTimeZone.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime endOfMonthUtc = endOfMonthUserTimeZone.withZoneSameInstant(ZoneOffset.UTC);

        // Query the bucket repository to get the time spent in the month bucket for the selected label and user
        LabelTimeBucket monthBucket = labelTimeBucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                viewer.getId(), labelId, MONTH, year, month
        ).orElse(null);

        // If monthBucket is null, return 0 for total time spent
        long totalTimeSpent = (monthBucket != null) ? monthBucket.getDurationMinutes() : 0;

        // Count the number of completed events for the selected label and month, adjusted for the user's time zone
        long totalEvents = eventRepository.countByLabelIdAndEventDateBetweenAndIsCompleted(
                labelId,
                startOfMonthUtc,
                endOfMonthUtc
        );

        // Return the stats in the DTO
        return new LabelMonthStatsDTO(label.getName(), totalEvents, totalTimeSpent);
    }

    @Override
    public List<LocalDate> getDatesByLabel(Long labelId, int year, int month) {
        // Fetch the user's time zone
        User viewer = authenticatedUserProvider.getCurrentUser();
        ZoneId userZoneId = ZoneId.of(viewer.getTimezone());

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new LabelNotFoundException(labelId));

        ownershipValidator.validateLabelOwnership(viewer.getId(), label);

        // Get the start and end of the month, adjusted for user's time zone
        ZonedDateTime startOfMonthUserTimeZone = LocalDate.of(year, month, 1).atStartOfDay(userZoneId);
        LocalDate lastDayOfMonth = LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());
        ZonedDateTime endOfMonthUserTimeZone = lastDayOfMonth.atTime(23, 59).atZone(userZoneId);

        // Convert the start and end of the month to UTC for the database query
        ZonedDateTime startOfMonthUtc = startOfMonthUserTimeZone.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime endOfMonthUtc = endOfMonthUserTimeZone.withZoneSameInstant(ZoneOffset.UTC);

        // Query the event repository to get events for the selected label and user in the given month
        List<Event> events = eventRepository.findByLabelIdAndEventDateBetweenAndIsCompleted(
                labelId,
                startOfMonthUtc,
                endOfMonthUtc
        );

        // Extract the unique event dates considering both startTime and endTime
        List<LocalDate> eventDates = events.stream()
                .flatMap(event -> {
                    // Start date
                    LocalDate startDate = event.getStartTime().toLocalDate();

                    // End date (if different from start date)
                    LocalDate endDate = (event.getEndTime() != null) ? event.getEndTime().toLocalDate() : startDate;

                    // If start and end dates are different, return both dates; otherwise, return the start date only
                    return (startDate.equals(endDate))
                            ? Stream.of(startDate)
                            : Stream.of(startDate, endDate);
                })
                .distinct()  // Ensure unique dates
                .collect(Collectors.toList());

        return eventDates;
    }

    @Override
    public List<LocalDate> getDatesWithEventsByMonth(Integer year, Integer month) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        ZoneId userZoneId = ZoneId.of(viewer.getTimezone());

        // If year or month are null, default to user's current year/month
        if (year == null || month == null) {
            ZonedDateTime now = ZonedDateTime.now(userZoneId);
            year = now.getYear();
            month = now.getMonthValue();
        }

        // Calculate start and end of month in user's timezone
        ZonedDateTime startOfMonthUser = LocalDate.of(year, month, 1).atStartOfDay(userZoneId);
        LocalDate lastDay = startOfMonthUser.toLocalDate()
                .withDayOfMonth(startOfMonthUser.toLocalDate().lengthOfMonth());
        ZonedDateTime endOfMonthUser = lastDay.atTime(23, 59).atZone(userZoneId);

        // Convert to UTC for DB queries
        ZonedDateTime startOfMonthUtc = startOfMonthUser.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime endOfMonthUtc = endOfMonthUser.withZoneSameInstant(ZoneOffset.UTC);

        // region --- Fetch confirmed scheduled events overlapping with this month ---
        List<Event> events = eventRepository.findConfirmedEventsForUserBetween(
                viewer.getId(),
                startOfMonthUtc,
                endOfMonthUtc
        );

        Stream<LocalDate> scheduledEventDates = events.stream()
                .flatMap(event -> {
                    LocalDate startDate = event.getStartTime().withZoneSameInstant(userZoneId).toLocalDate();
                    LocalDate endDate = event.getEndTime().withZoneSameInstant(userZoneId).toLocalDate();

                    return (startDate.equals(endDate))
                            ? Stream.of(startDate)
                            : Stream.of(startDate, endDate);
                });
        // endregion

        // region --- Fetch confirmed recurring events overlapping with this month via BO ---
        List<RecurringEvent> recurringEvents = recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                viewer.getId(),
                startOfMonthUser.toLocalDate(),
                endOfMonthUser.toLocalDate()
        );

        // Build list of all dates in the month
        List<LocalDate> datesInMonth = startOfMonthUser.toLocalDate()
                .datesUntil(endOfMonthUser.toLocalDate().plusDays(1))
                .collect(Collectors.toList());

        Set<LocalDate> recurringEventDates = new HashSet<>();
        for (RecurringEvent recurringEvent : recurringEvents) {
            ParsedRecurrenceInput parsed = recurringEvent.getRecurrenceRule().getParsed();

            for (LocalDate date : datesInMonth) {
                // Ensure date is within the recurring event's startDate and endDate range
                if ((date.isAfter(recurringEvent.getStartDate()) || date.isEqual(recurringEvent.getStartDate())) &&
                        (date.isBefore(recurringEvent.getEndDate()) || date.isEqual(recurringEvent.getEndDate()))) {

                    if (recurrenceRuleService.occursOn(parsed, date)) {
                        recurringEventDates.add(date);
                    }
                }
            }
        }
        // endregion

        // Combine scheduled and recurring dates
        return Stream.concat(scheduledEventDates, recurringEventDates.stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

}

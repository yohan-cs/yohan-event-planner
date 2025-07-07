package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.enums.TimeFilter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.ZonedDateTime;

/**
 * Filter used for searching events by label, time range, and completion state.
 *
 * <p>This DTO supports both self and public event searches. Certain fields are only honored
 * when the requester is querying their own data.</p>
 *
 * <p><b>Usage Notes:</b></p>
 * <ul>
 *   <li><b>Target user</b>: The user being queried is determined by the route path (e.g., {@code /{username}/events})
 *       and resolved in the service layer. This DTO does <b>not</b> include a {@code userId} and <b>does not</b> determine the user being queried.</li>
 *
 *   <li><b>labelId</b>:</li>
 *
 *   <li><b>timeFilter</b>: Required. Determines how to apply time-based filtering.
 *     <ul>
 *       <li>{@code ALL} – No time filtering; {@code start} and {@code end} are ignored.</li>
 *       <li>{@code PAST_ONLY} – Filters past events. {@code end} defaults to now; {@code start} is ignored.</li>
 *       <li>{@code FUTURE_ONLY} – Filters upcoming events. {@code start} defaults to now; {@code end} is ignored.</li>
 *       <li>{@code CUSTOM} – Applies {@code start} and {@code end} exactly as provided. Defaults to {@code FAR_PAST}/{@code FAR_FUTURE} if null.</li>
 *     </ul>
 *   </li>
 *
 *   <li><b>start</b> / <b>end</b>: Only used when {@code timeFilter == CUSTOM}.</li>
 *
 *   <li><b>sortDescending</b>: If {@code true}, sorts events by descending {@code startTime} (newest to oldest); otherwise ascending.</li>
 *
 *   <li><b>includeIncompletePastEvents</b>:
 *     <ul>
 *       <li>Only honored if the authenticated user is querying their own data.</li>
 *       <li>Ignored for public queries; defaults to {@code false}.</li>
 *     </ul>
 *   </li>
 * </ul>
 */

public record EventFilterDTO(

        @Positive
        Long labelId,

        @NotNull
        TimeFilter timeFilter,

        ZonedDateTime start,

        ZonedDateTime end,

        Boolean sortDescending,

        Boolean includeIncompletePastEvents

) {}

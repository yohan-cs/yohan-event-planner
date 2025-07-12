package com.yohan.event_planner.domain.enums;

/**
 * Enumeration of supported recurrence frequencies for recurring events.
 * 
 * <p>This enum defines the basic temporal patterns that can be used to create
 * recurring events. These frequencies serve as the foundation for more complex
 * recurrence rules and patterns in the event planning system.</p>
 * 
 * <h2>Frequency Patterns</h2>
 * <p>Each frequency represents a different temporal repetition pattern:</p>
 * <ul>
 *   <li><strong>DAILY</strong>: Events that repeat every day</li>
 *   <li><strong>WEEKLY</strong>: Events that repeat every week on the same day</li>
 *   <li><strong>MONTHLY</strong>: Events that repeat monthly on the same date</li>
 * </ul>
 * 
 * <h2>Usage in Recurrence Rules</h2>
 * <p>These frequencies are used as building blocks for:</p>
 * <ul>
 *   <li>Simple recurring event creation</li>
 *   <li>Complex recurrence rule parsing</li>
 *   <li>Interval-based recurrence (e.g., every 2 weeks)</li>
 *   <li>RRule standard compliance</li>
 * </ul>
 * 
 * <h2>Implementation Notes</h2>
 * <p>When combined with intervals, these frequencies support patterns like:</p>
 * <ul>
 *   <li>DAILY + interval 2 = every other day</li>
 *   <li>WEEKLY + interval 3 = every 3 weeks</li>
 *   <li>MONTHLY + interval 1 = every month</li>
 * </ul>
 * 
 * @see com.yohan.event_planner.domain.RecurringEvent
 * @see com.yohan.event_planner.domain.RecurrenceRuleVO
 */
public enum RecurrenceFrequency {
    
    /**
     * Daily recurrence frequency.
     * <p>Events repeat every day, providing the highest frequency of recurrence.
     * Commonly used for daily habits, routines, or regular activities.</p>
     */
    DAILY,
    
    /**
     * Weekly recurrence frequency.
     * <p>Events repeat every week on the same day of the week.
     * Popular for weekly meetings, classes, or recurring appointments.</p>
     */
    WEEKLY,
    
    /**
     * Monthly recurrence frequency.
     * <p>Events repeat every month on the same date.
     * Suitable for monthly reports, billing cycles, or periodic reviews.</p>
     */
    MONTHLY
}

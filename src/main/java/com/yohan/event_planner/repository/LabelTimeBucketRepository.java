package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.LabelTimeBucket;
import com.yohan.event_planner.domain.enums.TimeBucketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link LabelTimeBucket} entities with temporal aggregation support.
 * 
 * <p>This repository provides sophisticated data access functionality for time bucket management,
 * supporting multi-granular time tracking (day, week, month) across user labels. It enables
 * complex temporal queries for analytics, goal tracking, and time allocation reporting essential
 * for comprehensive time management within the event planning system.</p>
 * 
 * <h2>Core Query Categories</h2>
 * <ul>
 *   <li><strong>Temporal Queries</strong>: Time-based bucket retrieval by various granularities</li>
 *   <li><strong>Multi-Label Aggregation</strong>: Aggregate statistics across label collections</li>
 *   <li><strong>User-Scoped Queries</strong>: All queries naturally filter by user ownership</li>
 *   <li><strong>Batch Operations</strong>: Efficient operations for multiple buckets and labels</li>
 * </ul>
 * 
 * <h2>Time Bucket Architecture</h2>
 * <p>Sophisticated temporal data organization:</p>
 * <ul>
 *   <li><strong>Multi-Granularity Support</strong>: Day, week, and month bucket types</li>
 *   <li><strong>Hierarchical Organization</strong>: Year and bucket value combinations</li>
 *   <li><strong>Label Integration</strong>: Time tracking scoped to specific labels</li>
 *   <li><strong>User Isolation</strong>: Complete separation of user time data</li>
 * </ul>
 * 
 * <h2>Temporal Query Patterns</h2>
 * <p>Comprehensive time-based data access:</p>
 * <ul>
 *   <li><strong>Specific Bucket Lookup</strong>: Precise bucket identification by all dimensions</li>
 *   <li><strong>Range Queries</strong>: Multiple bucket values within year/type combinations</li>
 *   <li><strong>Cross-Label Aggregation</strong>: Statistics across multiple labels simultaneously</li>
 *   <li><strong>Historical Analysis</strong>: Time series data for trend analysis</li>
 * </ul>
 * 
 * <h2>Multi-Label Analytics</h2>
 * <p>Advanced analytics capabilities:</p>
 * <ul>
 *   <li><strong>Badge Support</strong>: Aggregate time across labels within badges</li>
 *   <li><strong>Comparative Analysis</strong>: Compare time allocation across labels</li>
 *   <li><strong>Batch Retrieval</strong>: Efficient collection queries for analytics</li>
 *   <li><strong>Statistical Foundations</strong>: Data for comprehensive reporting</li>
 * </ul>
 * 
 * <h2>Bucket Type Support</h2>
 * <p>Multiple temporal granularities:</p>
 * 
 * <h3>Daily Buckets (DAY)</h3>
 * <ul>
 *   <li><strong>Granularity</strong>: Day-of-year (1-366)</li>
 *   <li><strong>Use Case</strong>: Detailed daily time tracking</li>
 *   <li><strong>Analytics</strong>: Daily habit tracking and patterns</li>
 * </ul>
 * 
 * <h3>Weekly Buckets (WEEK)</h3>
 * <ul>
 *   <li><strong>Granularity</strong>: ISO week number (1-53)</li>
 *   <li><strong>Use Case</strong>: Weekly goal tracking</li>
 *   <li><strong>Analytics</strong>: Weekly productivity patterns</li>
 * </ul>
 * 
 * <h3>Monthly Buckets (MONTH)</h3>
 * <ul>
 *   <li><strong>Granularity</strong>: Month number (1-12)</li>
 *   <li><strong>Use Case</strong>: Monthly progress tracking</li>
 *   <li><strong>Analytics</strong>: Long-term trend analysis</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <ul>
 *   <li><strong>Composite Indexing</strong>: Multi-column indexes for complex queries</li>
 *   <li><strong>Batch Operations</strong>: Collection-based queries for efficiency</li>
 *   <li><strong>Targeted Retrieval</strong>: Precise bucket identification without scanning</li>
 *   <li><strong>Aggregation Support</strong>: Database-level aggregation where possible</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This repository integrates with:</p>
 * <ul>
 *   <li><strong>LabelTimeBucketService</strong>: Primary service layer integration</li>
 *   <li><strong>BadgeStatsService</strong>: Multi-label analytics and aggregation</li>
 *   <li><strong>CalendarService</strong>: Monthly view generation with statistics</li>
 *   <li><strong>EventService</strong>: Time tracking integration with event completion</li>
 * </ul>
 * 
 * <h2>Query Complexity Management</h2>
 * <p>Handle complex temporal queries efficiently:</p>
 * <ul>
 *   <li><strong>Parameter Optimization</strong>: Efficient parameter binding</li>
 *   <li><strong>Index Utilization</strong>: Leverage composite indexes for performance</li>
 *   <li><strong>Result Set Optimization</strong>: Minimize data transfer for large queries</li>
 *   <li><strong>Memory Management</strong>: Efficient handling of bulk operations</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <ul>
 *   <li><strong>Temporal Integrity</strong>: Maintain consistent bucket values</li>
 *   <li><strong>User Scoping</strong>: Reliable user-bucket associations</li>
 *   <li><strong>Label Relationships</strong>: Consistent label-bucket connections</li>
 *   <li><strong>Duration Accuracy</strong>: Precise time tracking data</li>
 * </ul>
 * 
 * <h2>Analytics Support</h2>
 * <p>Foundation for comprehensive time analytics:</p>
 * <ul>
 *   <li><strong>Trend Analysis</strong>: Historical time allocation patterns</li>
 *   <li><strong>Goal Tracking</strong>: Progress monitoring across time periods</li>
 *   <li><strong>Comparative Analysis</strong>: Label-to-label time comparisons</li>
 *   <li><strong>Productivity Metrics</strong>: Time efficiency calculations</li>
 * </ul>
 * 
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><strong>User Data Isolation</strong>: Complete separation of user time data</li>
 *   <li><strong>Privacy Protection</strong>: Prevent cross-user time data access</li>
 *   <li><strong>Authorization Support</strong>: Enable service-layer security enforcement</li>
 *   <li><strong>Data Confidentiality</strong>: Protect sensitive time tracking information</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>User Scoping</strong>: All queries must include user ID for proper isolation</li>
 *   <li><strong>Bucket Precision</strong>: Ensure accurate bucket value calculations</li>
 *   <li><strong>Performance Impact</strong>: Complex queries may require optimization</li>
 *   <li><strong>Data Volume</strong>: Consider data growth impact on query performance</li>
 * </ul>
 * 
 * @see LabelTimeBucket
 * @see LabelTimeBucketService
 * @see TimeBucketType
 * @see BadgeStatsService
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public interface LabelTimeBucketRepository extends JpaRepository<LabelTimeBucket, Long> {
    Optional<LabelTimeBucket> findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
            Long userId, Long labelId, TimeBucketType bucketType, int bucketYear, int bucketValue
    );

    List<LabelTimeBucket> findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
            Long userId,
            Collection<Long> labelIds,
            TimeBucketType bucketType,
            int bucketYear,
            List<Integer> bucketValues
    );

    List<LabelTimeBucket> findByUserIdAndLabelIdIn(
            Long userId,
            Collection<Long> labelIds
    );
}

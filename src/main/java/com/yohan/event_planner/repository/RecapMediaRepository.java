package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.RecapMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing {@link RecapMedia} entities with specialized media operations.
 * 
 * <p>This repository handles the persistence layer for multimedia content associated with event
 * recaps, providing optimized queries for media ordering, bulk operations, and content management.
 * It supports the storage and retrieval of multimedia content that enriches event documentation.</p>
 * 
 * <h2>Core Query Categories</h2>
 * <ul>
 *   <li><strong>Ordered Retrieval</strong>: Fetch media items in their intended display sequence</li>
 *   <li><strong>Bulk Operations</strong>: Count, find, and delete operations by recap</li>
 *   <li><strong>Content Management</strong>: Support for media reordering and lifecycle operations</li>
 * </ul>
 * 
 * <h2>Key Functionality</h2>
 * <ul>
 *   <li><strong>Ordered Media Lists</strong>: {@link #findByRecapIdOrderByMediaOrder(Long)} for consistent display</li>
 *   <li><strong>Unordered Access</strong>: {@link #findByRecapId(Long)} for bulk operations</li>
 *   <li><strong>Count Operations</strong>: {@link #countByRecapId(Long)} for pagination and limits</li>
 *   <li><strong>Bulk Deletion</strong>: {@link #deleteByRecapId(Long)} for recap cleanup</li>
 * </ul>
 * 
 * <h2>Media Type Support</h2>
 * <p>The repository supports multiple media types:</p>
 * <ul>
 *   <li><strong>IMAGE</strong>: Static images, photos, screenshots</li>
 *   <li><strong>VIDEO</strong>: Video content with optional duration tracking</li>
 *   <li><strong>AUDIO</strong>: Audio recordings with optional duration metadata</li>
 * </ul>
 * 
 * <h2>Ordering and Presentation</h2>
 * <ul>
 *   <li><strong>Explicit Ordering</strong>: mediaOrder field controls display sequence</li>
 *   <li><strong>Consistent Presentation</strong>: Lower order values appear first</li>
 *   <li><strong>Reordering Support</strong>: Update mediaOrder without changing database IDs</li>
 *   <li><strong>Narrative Flow</strong>: Maintain intended story sequence in recaps</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Indexed Queries</strong>: recap_id indexed for efficient lookups</li>
 *   <li><strong>Ordered Queries</strong>: Optimize for mediaOrder-based sorting</li>
 *   <li><strong>Bulk Operations</strong>: Efficient count and delete operations</li>
 *   <li><strong>Lazy Loading</strong>: EventRecap relationships loaded on demand</li>
 * </ul>
 * 
 * <h2>Storage Strategy</h2>
 * <ul>
 *   <li><strong>URL-Based Storage</strong>: Media files stored externally with URL references</li>
 *   <li><strong>No Binary Data</strong>: Database stores metadata only, not file content</li>
 *   <li><strong>External CDN Integration</strong>: Support for cloud storage and CDN URLs</li>
 *   <li><strong>Flexible Hosting</strong>: Compatible with various media hosting solutions</li>
 * </ul>
 * 
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><strong>Ownership Validation</strong>: Media access controlled through recap ownership</li>
 *   <li><strong>URL Validation</strong>: Ensure media URLs are accessible and properly formatted</li>
 *   <li><strong>Content Security</strong>: Integration with service layer for media validation</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This repository integrates with:</p>
 * <ul>
 *   <li><strong>RecapMediaService</strong>: Primary service layer integration</li>
 *   <li><strong>EventRecapService</strong>: Recap-media relationship management</li>
 *   <li><strong>External Storage</strong>: CDN and cloud storage integration</li>
 *   <li><strong>Security Framework</strong>: Ownership and authorization support</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li><strong>Gallery Creation</strong>: Ordered media lists for recap galleries</li>
 *   <li><strong>Content Management</strong>: Add, remove, and reorder media attachments</li>
 *   <li><strong>Bulk Operations</strong>: Efficient handling of media collections</li>
 *   <li><strong>Analytics Support</strong>: Count and analyze media usage patterns</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Tight Coupling</strong>: All media items are tightly coupled to their parent recap</li>
 *   <li><strong>Cascade Operations</strong>: Bulk deletion cascades automatically when recaps are deleted</li>
 *   <li><strong>Application Ordering</strong>: Media ordering is application-managed, not database-enforced</li>
 *   <li><strong>URL Accessibility</strong>: URLs must remain accessible for media to display correctly</li>
 * </ul>
 * 
 * @see RecapMedia
 * @see com.yohan.event_planner.domain.EventRecap
 * @see com.yohan.event_planner.service.RecapMediaService
 * @see com.yohan.event_planner.domain.enums.RecapMediaType
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Repository
public interface RecapMediaRepository extends JpaRepository<RecapMedia, Long> {

    /**
     * Finds all media items for a given recap, ordered by their mediaOrder field.
     *
     * @param recapId the ID of the recap
     * @return list of media items ordered by mediaOrder
     */
    List<RecapMedia> findByRecapIdOrderByMediaOrder(Long recapId);

    /**
     * Finds all media items for a given recap without enforcing order.
     *
     * @param recapId the ID of the recap
     * @return list of media items
     */
    List<RecapMedia> findByRecapId(Long recapId);

    /**
     * Counts the number of media items attached to a recap.
     *
     * @param recapId the ID of the recap
     * @return the count of media items
     */
    int countByRecapId(Long recapId);

    /**
     * Deletes all media items associated with the given recap ID.
     *
     * @param recapId the ID of the recap
     */
    void deleteByRecapId(Long recapId);
}

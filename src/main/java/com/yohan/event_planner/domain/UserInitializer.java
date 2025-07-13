package com.yohan.event_planner.domain;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.enums.LabelColor;
import com.yohan.event_planner.repository.LabelRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service component responsible for performing user initialization tasks during account creation.
 * 
 * <p>This component handles the setup of essential user-specific data that must be created
 * immediately after user account creation. It ensures that new users have all necessary
 * default entities and relationships established for proper system operation.</p>
 * 
 * <h2>Initialization Responsibilities</h2>
 * <p>The primary responsibility is creating the default "Unlabeled" label:</p>
 * <ul>
 *   <li><strong>Default label creation</strong>: Creates a special "Unlabeled" label for each user</li>
 *   <li><strong>Label assignment</strong>: Links the label to the user as their default unlabeled category</li>
 *   <li><strong>System consistency</strong>: Ensures all users have a fallback label for uncategorized events</li>
 * </ul>
 * 
 * <h2>Default Label Purpose</h2>
 * <p>The "Unlabeled" label serves several critical functions:</p>
 * <ul>
 *   <li><strong>Fallback categorization</strong>: Events without explicit labels are assigned to this category</li>
 *   <li><strong>System integrity</strong>: Prevents orphaned events that lack label associations</li>
 *   <li><strong>User experience</strong>: Provides immediate categorization capability for new users</li>
 *   <li><strong>Analytics continuity</strong>: Ensures all events can be included in time tracking and reports</li>
 * </ul>
 * 
 * <h2>Transactional Behavior</h2>
 * <p>User initialization is performed within a single transaction to ensure atomicity:</p>
 * <ul>
 *   <li>User creation and label creation succeed or fail together</li>
 *   <li>Prevents partial initialization states that could cause system inconsistencies</li>
 *   <li>Enables rollback if any initialization step fails</li>
 * </ul>
 * 
 * <h2>Usage Context</h2>
 * <p>This component is typically invoked during:</p>
 * <ul>
 *   <li><strong>User registration</strong>: As part of the account creation workflow</li>
 *   <li><strong>Administrative user creation</strong>: When creating users programmatically</li>
 *   <li><strong>System onboarding</strong>: Ensuring new users have proper initial state</li>
 * </ul>
 * 
 * <h2>Dependencies</h2>
 * <p>Requires the following components:</p>
 * <ul>
 *   <li><strong>UserBO</strong>: For user persistence and business logic</li>
 *   <li><strong>LabelRepository</strong>: For direct label creation and persistence</li>
 * </ul>
 * 
 * @see User
 * @see Label
 * @see UserBO
 */
@Component
public class UserInitializer {

    /** Business object for user creation and management operations. */
    private final UserBO userBO;
    
    /** Repository for direct label persistence operations. */
    private final LabelRepository labelRepository;

    /**
     * Creates a new UserInitializer with the required dependencies.
     * 
     * @param userBO business object for user operations
     * @param labelRepository repository for label persistence
     */
    public UserInitializer(UserBO userBO, LabelRepository labelRepository) {
        this.userBO = userBO;
        this.labelRepository = labelRepository;
    }

    /**
     * Initializes a new user account with essential default data.
     * 
     * <p>This method performs the complete user initialization workflow:</p>
     * <ol>
     *   <li>Persists the user account through the business layer</li>
     *   <li>Creates a default "Unlabeled" label for the user</li>
     *   <li>Persists the label to the database</li>
     *   <li>Assigns the label as the user's default unlabeled category</li>
     *   <li>Updates the user with the label assignment</li>
     * </ol>
     * 
     * <p>All operations are performed within a single transaction to ensure atomicity.
     * If any step fails, the entire initialization is rolled back.</p>
     * 
     * @param user the user account to initialize (typically a new, unsaved user)
     * @return the fully initialized and persisted user with default label assigned
     * @throws RuntimeException if any step of the initialization fails
     */
    @Transactional
    public User initializeUser(User user) {
        User savedUser = userBO.createUser(user);

        Label unlabeled = new Label("Unlabeled", LabelColor.GRAY, savedUser);

        labelRepository.save(unlabeled);

        savedUser.assignUnlabeled(unlabeled);

        return userBO.updateUser(savedUser);
    }
}

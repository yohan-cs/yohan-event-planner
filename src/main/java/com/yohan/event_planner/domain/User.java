package com.yohan.event_planner.domain;

import com.yohan.event_planner.constants.ApplicationConstants;
import com.yohan.event_planner.domain.enums.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Entity representing a registered user of the application.
 *
 * <p>This entity supports core account functionality including registration, authentication,
 * soft deletion, role-based authorization, profile visibility, timezone tracking, and 
 * impromptu event pinning for dashboard reminders.</p>
 *
 * <p>All persistence is managed via JPA. User records are stored in the {@code users} table,
 * and associated roles are stored in the {@code user_roles} table via an element collection.
 * The pinned impromptu event relationship is stored as a foreign key reference.</p>
 * 
 * <h2>Impromptu Event Pinning</h2>
 * <p>Users can pin one impromptu event at a time as a dashboard reminder:</p>
 * <ul>
 *   <li><strong>Single Pin Limit</strong>: Only one impromptu event can be pinned per user</li>
 *   <li><strong>Automatic Pinning</strong>: New impromptu events are automatically pinned when created</li>
 *   <li><strong>Qualification Rules</strong>: Only events with {@code draft = true} and {@code impromptu = true} qualify</li>
 *   <li><strong>Auto-Unpin Safeguards</strong>: Events are automatically unpinned when confirmed, completed, or deleted</li>
 *   <li><strong>Manual Unpinning</strong>: Users can manually unpin events through GraphQL mutations</li>
 *   <li><strong>Privacy Protection</strong>: Pinned events are only visible to the profile owner</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <p>The pinned event relationship includes multiple safeguards:</p>
 * <ul>
 *   <li><strong>Real-time Validation</strong>: Pinned events are validated during profile retrieval</li>
 *   <li><strong>Automatic Cleanup</strong>: Invalid pinned events are automatically cleared</li>
 *   <li><strong>Transaction Safety</strong>: All pinning operations are properly transactional</li>
 *   <li><strong>Referential Integrity</strong>: Foreign key constraints ensure data consistency</li>
 * </ul>
 */
@Entity
@Table(name = "users")
public class User {

    /** Number of days before a pending deletion is permanently applied. */
    private static final long DELETION_GRACE_PERIOD_DAYS = ApplicationConstants.USER_DELETION_GRACE_PERIOD_DAYS;

    /**
     * Unique identifier for the user. Auto-generated by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique, case-insensitive username used for login and identification.
     */
    @Column(nullable = false, unique = true, length = ApplicationConstants.SHORT_NAME_MAX_LENGTH)
    private String username;

    /**
     * The hashed password.
     */
    @Column(name = "hashedpassword", nullable = false)
    private String hashedPassword;

    /**
     * Unique, valid email address used for communication and login.
     */
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * User’s first name.
     */
    @Column(name = "firstname", nullable = false, length = ApplicationConstants.SHORT_NAME_MAX_LENGTH)
    private String firstName;

    /**
     * User’s last name.
     */
    @Column(name = "lastname", nullable = false, length = ApplicationConstants.SHORT_NAME_MAX_LENGTH)
    private String lastName;

    /**
     * Timezone ID associated with the user, used for event scheduling and notifications.
     */
    @Column(nullable = false)
    private String timezone;

    /**
     * Optional user-provided biography or personal description.
     */
    @Column(name = "bio", length = ApplicationConstants.LONG_TEXT_MAX_LENGTH)
    private String bio;

    /**
     * URL to the user's profile picture (e.g., Firebase or CDN link).
     */
    @Column(name = "profile_picture_url", length = ApplicationConstants.VERY_LONG_URL_MAX_LENGTH)
    private String profilePictureUrl;

    /**
     * Special fallback label used for events with no visible label.
     *
     * <p>
     * This label is automatically created when the user is constructed and is
     * not visible or editable from the frontend. It serves as a hidden default
     * bucket for "no label" events.
     * </p>
     *
     * <p>
     * The {@code unlabeled} label is deleted automatically when the user is deleted.
     * </p>
     */
    @OneToOne
    @JoinColumn(name = "unlabeled_id", nullable = true)
    private Label unlabeled;

    /**
     * The currently pinned impromptu event that serves as a visual reminder on the user's dashboard.
     *
     * <p>
     * Only one impromptu event can be pinned at a time. An impromptu event is eligible for pinning
     * if it has {@code draft = true} and {@code impromptu = true}. When an impromptu event is
     * published (draft = false) or deleted, it is automatically unpinned.
     * </p>
     *
     * <p>
     * This field is not included in any UserResponseDTO to keep internal state separate from
     * API responses. Use dedicated service methods to manage pinned events.
     * </p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_event_id", referencedColumnName = "id")
    private Event pinnedImpromptuEvent;

    /**
     * Role-based access control.
     * <p>Each user is assigned one or more roles (e.g., USER, MOD, ADMIN).
     * Default is {@link Role#USER}.</p>
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<Role> roles = new HashSet<>(Set.of(Role.USER));

    /**
     * Indicates whether the user has scheduled their account for deletion.
     *
     * <p>When true, the user is considered inactive and will be permanently deleted after
     * the {@link #scheduledDeletionDate} grace period has passed.</p>
     */
    @Column(name = "pending_deletion", nullable = false)
    private boolean isPendingDeletion = false;

    /**
     * The date and time after which the user's account is eligible for permanent deletion.
     *
     * <p>This value is set at the time of deletion request (e.g., 30 days in the future).</p>
     */
    @Column(name = "scheduled_deletion_date")
    private java.time.ZonedDateTime scheduledDeletionDate;

    /**
     * Indicates whether the user's email address has been verified.
     *
     * <p>Users must verify their email before they can log in and use the application.
     * This field is set to false by default and updated to true when the user
     * successfully verifies their email through the verification link.</p>
     */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    /**
     * The timestamp when this user account was created.
     *
     * <p>This field is automatically set to the current time when the user is first created.
     * It is used for account lifecycle management, including cleanup of unverified accounts
     * that have exceeded the verification time limit.</p>
     */
    @Column(name = "created_at", nullable = false)
    private java.time.ZonedDateTime createdAt;

    /**
     * Default constructor required by JPA.
     */
    protected User() {
        // for JPA
    }

    /**
     * Constructs a new user instance with mandatory fields.
     *
     * @param username       unique username
     * @param hashedPassword hashed password
     * @param email          unique email address
     * @param firstName      user's first name
     * @param lastName       user's last name
     * @param timezone       user's timezone ID
     */
    public User(String username, String hashedPassword, String email, String firstName, String lastName, String timezone) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.timezone = timezone;
        this.createdAt = ZonedDateTime.now();
    }

    // --- Getters ---
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public String getBio() {
        return bio;
    }

    public Label getUnlabeled() {
        return unlabeled;
    }

    public Event getPinnedImpromptuEvent() {
        return pinnedImpromptuEvent;
    }

    public Set<Role> getRoles() {
        return Set.copyOf(roles);
    }

    public boolean isPendingDeletion() {
        return isPendingDeletion;
    }

    public Optional<ZonedDateTime> getScheduledDeletionDate() {
        return Optional.ofNullable(scheduledDeletionDate);
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    // --- Setters ---
    public void setUsername(String username) {
        this.username = username;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    void assignUnlabeled(Label unlabeled) {
        this.unlabeled = unlabeled;
    }

    public void setPinnedImpromptuEvent(Event pinnedImpromptuEvent) {
        this.pinnedImpromptuEvent = pinnedImpromptuEvent;
    }

    /**
     * Marks the user for deletion and sets the scheduled deletion date.
     * The scheduled deletion date is calculated based on the static grace period.
     */
    public void markForDeletion(ZonedDateTime currentTime) {
        this.isPendingDeletion = true;
        this.scheduledDeletionDate = currentTime.plusDays(DELETION_GRACE_PERIOD_DAYS);
    }
    /**
     * Cancels the deletion request and clears the scheduled deletion date.
     */
    public void unmarkForDeletion() {
        this.isPendingDeletion = false;
        this.scheduledDeletionDate = null;
    }

    /**
     * Marks the user's email as verified.
     * This allows the user to log in and use the application.
     */
    public void verifyEmail() {
        this.emailVerified = true;
    }

    // --- Equality & Hashing ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;

        if (id != null && other.getId() != null) {
            return id.equals(other.getId());
        }

        return username != null && email != null &&
                username.equals(other.getUsername()) &&
                email.equals(other.getEmail());
    }

    @Override
    public int hashCode() {
        if (id != null) return id.hashCode();
        return (username != null ? username.hashCode() : 0) ^ (email != null ? email.hashCode() : 0);
    }
}

package com.yohan.event_planner.domain.enums;

/**
 * Enumeration of user roles within the system.
 * <p>
 * Each role corresponds to a specific authority string used
 * for authorization and access control.
 * </p>
 */
public enum Role {

    /**
     * Standard user role with basic privileges.
     */
    USER("ROLE_USER"),

    /**
     * Moderator role with elevated privileges, such as content moderation.
     */
    MODERATOR("ROLE_MODERATOR"),

    /**
     * Administrator role with full system access and management rights.
     */
    ADMIN("ROLE_ADMIN");

    /**
     * The authority string representing the role.
     * This string is used for role-based access control (RBAC).
     */
    private final String authority;

    /**
     * Constructs a Role enum with the specified authority string.
     *
     * @param authority the authority string corresponding to this role
     */
    Role(String authority) {
        this.authority = authority;
    }

    /**
     * Returns the authority string associated with this role.
     * <p>This string is used to control access to resources based on user roles.</p>
     *
     * @return the authority string
     */
    public String getAuthority() {
        return authority;
    }
}

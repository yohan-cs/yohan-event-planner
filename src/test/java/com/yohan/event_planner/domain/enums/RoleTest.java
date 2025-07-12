package com.yohan.event_planner.domain.enums;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Nested
    class AuthorityMapping {

        @Test
        void user_shouldHaveCorrectAuthority() {
            assertThat(Role.USER.getAuthority()).isEqualTo("ROLE_USER");
        }

        @Test
        void moderator_shouldHaveCorrectAuthority() {
            assertThat(Role.MODERATOR.getAuthority()).isEqualTo("ROLE_MODERATOR");
        }

        @Test
        void admin_shouldHaveCorrectAuthority() {
            assertThat(Role.ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
        }
    }

    @Nested
    class EnumValues {

        @Test
        void allRoles_shouldBePresent() {
            Role[] roles = Role.values();

            assertThat(roles).hasSize(3);
            assertThat(roles).containsExactly(Role.USER, Role.MODERATOR, Role.ADMIN);
        }

        @Test
        void valueOf_shouldReturnCorrectRole() {
            assertThat(Role.valueOf("USER")).isEqualTo(Role.USER);
            assertThat(Role.valueOf("MODERATOR")).isEqualTo(Role.MODERATOR);
            assertThat(Role.valueOf("ADMIN")).isEqualTo(Role.ADMIN);
        }

        @Test
        void name_shouldReturnCorrectName() {
            assertThat(Role.USER.name()).isEqualTo("USER");
            assertThat(Role.MODERATOR.name()).isEqualTo("MODERATOR");
            assertThat(Role.ADMIN.name()).isEqualTo("ADMIN");
        }
    }

    @Nested
    class SecurityIntegration {

        @Test
        void authorityStrings_shouldFollowSpringSecurityConvention() {
            // Spring Security convention is ROLE_ prefix
            for (Role role : Role.values()) {
                assertThat(role.getAuthority()).startsWith("ROLE_");
            }
        }

        @Test
        void authorityStrings_shouldBeUnique() {
            String[] authorities = {
                Role.USER.getAuthority(),
                Role.MODERATOR.getAuthority(),
                Role.ADMIN.getAuthority()
            };

            // Check that all authorities are unique
            assertThat(authorities).doesNotHaveDuplicates();
        }

        @Test
        void authorityStrings_shouldNotBeNullOrEmpty() {
            for (Role role : Role.values()) {
                assertThat(role.getAuthority()).isNotNull();
                assertThat(role.getAuthority()).isNotEmpty();
                assertThat(role.getAuthority()).isNotBlank();
            }
        }
    }

    @Nested
    class BusinessLogic {

        @Test
        void getAuthority_shouldReturnConsistentResults() {
            // Multiple calls should return the same result
            Role role = Role.USER;
            String authority1 = role.getAuthority();
            String authority2 = role.getAuthority();

            assertThat(authority1).isEqualTo(authority2);
            assertThat(authority1).isSameAs(authority2); // Should be the same String instance
        }

        @Test
        void enumEquality_shouldWorkCorrectly() {
            Role user1 = Role.USER;
            Role user2 = Role.valueOf("USER");

            assertThat(user1).isEqualTo(user2);
            assertThat(user1).isSameAs(user2); // Enum instances are singletons
        }

        @Test
        void enumOrdering_shouldBeConsistent() {
            // Verify the order is USER, MODERATOR, ADMIN
            assertThat(Role.USER.ordinal()).isEqualTo(0);
            assertThat(Role.MODERATOR.ordinal()).isEqualTo(1);
            assertThat(Role.ADMIN.ordinal()).isEqualTo(2);
        }
    }

    @Nested
    class StringRepresentation {

        @Test
        void toString_shouldReturnEnumName() {
            assertThat(Role.USER.toString()).isEqualTo("USER");
            assertThat(Role.MODERATOR.toString()).isEqualTo("MODERATOR");
            assertThat(Role.ADMIN.toString()).isEqualTo("ADMIN");
        }

        @Test
        void authorityString_shouldMatchExpectedFormat() {
            // Authority should be ROLE_ + enum name
            assertThat(Role.USER.getAuthority()).isEqualTo("ROLE_" + Role.USER.name());
            assertThat(Role.MODERATOR.getAuthority()).isEqualTo("ROLE_" + Role.MODERATOR.name());
            assertThat(Role.ADMIN.getAuthority()).isEqualTo("ROLE_" + Role.ADMIN.name());
        }
    }

    @Nested
    class HierarchicalConsiderations {

        @Test
        void roleHierarchy_shouldReflectPrivilegeLevels() {
            // While the enum doesn't enforce hierarchy, the ordering should reflect privilege levels
            // USER (lowest privileges) -> MODERATOR (moderate privileges) -> ADMIN (highest privileges)
            
            assertThat(Role.USER.ordinal()).isLessThan(Role.MODERATOR.ordinal());
            assertThat(Role.MODERATOR.ordinal()).isLessThan(Role.ADMIN.ordinal());
        }

        @Test
        void allRoles_shouldHaveDistinctAuthorities() {
            // Each role should have a unique authority string for proper access control
            assertThat(Role.USER.getAuthority()).isNotEqualTo(Role.MODERATOR.getAuthority());
            assertThat(Role.MODERATOR.getAuthority()).isNotEqualTo(Role.ADMIN.getAuthority());
            assertThat(Role.USER.getAuthority()).isNotEqualTo(Role.ADMIN.getAuthority());
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void enumComparison_shouldWorkWithSwitchStatements() {
            // Verify that roles work correctly in switch statements
            for (Role role : Role.values()) {
                String result = switch (role) {
                    case USER -> "basic";
                    case MODERATOR -> "elevated";
                    case ADMIN -> "full";
                };
                
                assertThat(result).isNotNull();
                assertThat(result).isIn("basic", "elevated", "full");
            }
        }

        @Test
        void roleCollection_shouldSupportStandardOperations() {
            java.util.Set<Role> roleSet = java.util.EnumSet.allOf(Role.class);
            
            assertThat(roleSet).hasSize(3);
            assertThat(roleSet).contains(Role.USER, Role.MODERATOR, Role.ADMIN);
        }

        @Test
        void roleAuthorities_shouldBeSuitableForSerialization() {
            // Authorities should be simple strings suitable for JSON/database storage
            for (Role role : Role.values()) {
                String authority = role.getAuthority();
                
                assertThat(authority).matches("^[A-Z_]+$"); // Only uppercase letters and underscores
                assertThat(authority).doesNotContain(" "); // No spaces
                assertThat(authority).doesNotContain("\t"); // No tabs
                assertThat(authority).doesNotContain("\n"); // No newlines
            }
        }
    }
}
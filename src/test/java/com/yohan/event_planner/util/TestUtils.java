package com.yohan.event_planner.util;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.auth.RegisterRequestDTO;
import com.yohan.event_planner.security.CustomUserDetails;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class TestUtils {

    public static UserCreateDTO createValidUserCreateDTO() {
        return new UserCreateDTO(
                TestConstants.VALID_USERNAME,
                TestConstants.VALID_PASSWORD,
                TestConstants.VALID_EMAIL,
                TestConstants.VALID_FIRST_NAME,
                TestConstants.VALID_LAST_NAME,
                TestConstants.VALID_TIMEZONE
        );
    }

    /**
     * Creates a static valid registration payload using constants.
     * Use this only when you are not concerned about uniqueness.
     */
    public static RegisterRequestDTO createValidRegisterPayload() {
        return new RegisterRequestDTO(
                TestConstants.VALID_USERNAME,
                TestConstants.VALID_PASSWORD,
                TestConstants.VALID_EMAIL,
                TestConstants.VALID_FIRST_NAME,
                TestConstants.VALID_LAST_NAME,
                TestConstants.VALID_TIMEZONE
        );
    }

    /**
     * Creates a valid registration payload with a unique suffix to prevent
     * username/email conflicts in integration tests.
     *
     * @param suffix a string to append to username and email
     * @return a new RegisterRequestDTO with unique credentials
     */
    public static RegisterRequestDTO createValidRegisterPayload(String suffix) {
        return new RegisterRequestDTO(
                "user" + suffix,
                TestConstants.VALID_PASSWORD,
                "user" + suffix + "@example.com",
                TestConstants.VALID_FIRST_NAME,
                TestConstants.VALID_LAST_NAME,
                TestConstants.VALID_TIMEZONE
        );
    }

    public static User createUserEntityWithoutId() {
        return new User(
                TestConstants.VALID_USERNAME,
                TestConstants.VALID_PASSWORD,
                TestConstants.VALID_EMAIL,
                TestConstants.VALID_FIRST_NAME,
                TestConstants.VALID_LAST_NAME,
                TestConstants.VALID_TIMEZONE
        );
    }

    public static User createUserEntityWithId() {
        return createUserEntityWithId(TestConstants.USER_ID);
    }

    public static User createUserEntityWithId(Long id) {
        User user = createUserEntityWithoutId();
        setUserId(user, id);
        return user;
    }

    public static CustomUserDetails createCustomUserDetails() {
        return new CustomUserDetails(createUserEntityWithId());
    }

    private static void setUserId(User user, Long id) {
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to set user ID in test", e);
        }
    }
}

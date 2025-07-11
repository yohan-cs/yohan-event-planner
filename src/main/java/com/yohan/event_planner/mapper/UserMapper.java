package com.yohan.event_planner.mapper;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between {@link User} domain entities and various DTOs
 * used throughout the application.
 *
 * <p>
 * This mapper handles transformations for:
 * <ul>
 *     <li>Internal user creation flow</li>
 *     <li>Partial user updates (patching)</li>
 *     <li>User response serialization</li>
 * </ul>
 * </p>
 *
 * <p><strong>Security Note:</strong>
 * Password fields are explicitly excluded from automatic mapping and must be
 * handled manually in the service or business layer where proper hashing
 * and validation can be applied.
 * </p>
 *
 * <p><strong>Null handling:</strong>
 * Fields in {@link UserUpdateDTO} that are {@code null} will be ignored
 * during patching, enabling partial updates without overwriting existing values.
 * </p>
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    /**
     * Converts a {@link UserCreateDTO} to a {@link User} entity.
     *
     * <p>
     * The resulting entity will have all fields from the DTO mapped over,
     * except for the password which must be hashed and set manually.
     * </p>
     *
     * @param dto the user creation data
     * @return a new {@link User} entity
     */
    @Mapping(target = "hashedPassword", ignore = true)
    User toEntity(UserCreateDTO dto);


    /**
     * Applies the non-null fields from a {@link UserUpdateDTO} to an existing {@link User} entity.
     *
     * <p>
     * This method is used to apply patch updates. Only fields that are not null in the DTO
     * will overwrite corresponding fields in the existing entity.
     * </p>
     *
     * @param user the entity to update
     * @param dto the patch data
     */
    @Mapping(target = "hashedPassword", ignore = true)
    void updateEntity(@MappingTarget User user, UserUpdateDTO dto);

    /**
     * Converts a {@link User} entity into a {@link UserResponseDTO}
     * for client-facing views (e.g., profile information).
     *
     * <p>
     * This strips away sensitive internal data and formats
     * the user object for safe return to clients.
     * </p>
     *
     * @param user the source entity
     * @return the user response DTO
     */
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "timezone", source = "timezone")
    UserResponseDTO toResponseDTO(User user);
}


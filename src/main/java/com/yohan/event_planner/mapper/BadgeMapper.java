package com.yohan.event_planner.mapper;

import com.yohan.event_planner.domain.Badge;
import com.yohan.event_planner.dto.BadgeCreateDTO;
import com.yohan.event_planner.dto.BadgeLabelDTO;
import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.BadgeUpdateDTO;
import com.yohan.event_planner.dto.TimeStatsDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Set;

/**
 * MapStruct mapper for converting between {@link Badge} domain entities and related DTOs.
 *
 * <p>
 * This mapper supports the following transformations:
 * <ul>
 *     <li>Creating new {@link Badge} entities from {@link BadgeCreateDTO}</li>
 *     <li>Applying partial updates using {@link BadgeUpdateDTO}</li>
 *     <li>Converting {@link Badge} entities to {@link BadgeResponseDTO}, including optional time stats</li>
 * </ul>
 * </p>
 *
 * <p><strong>Null handling:</strong>
 * Fields in {@link BadgeUpdateDTO} that are {@code null} will be ignored during patch operations.
 * </p>
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BadgeMapper {

    /**
     * Converts a {@link BadgeCreateDTO} to a new {@link Badge} entity.
     *
     * <p>This method performs the initial mapping for badge creation, converting
     * the input DTO to a domain entity. Note that the resulting entity will not
     * have an ID (as it hasn't been persisted) and requires additional setup
     * such as user assignment and sort order determination.</p>
     *
     * <h3>Mapping Behavior:</h3>
     * <ul>
     *   <li><strong>Name</strong>: Directly mapped from DTO to entity</li>
     *   <li><strong>Label IDs</strong>: Mapped to entity's labelIds field</li>
     *   <li><strong>Excluded Fields</strong>: ID, user, sortOrder, labelOrder (set by service layer)</li>
     * </ul>
     *
     * @param dto the badge creation data
     * @return a new Badge entity ready for service layer processing
     */
    Badge toEntity(BadgeCreateDTO dto);

    /**
     * Updates an existing {@link Badge} entity with data from {@link BadgeUpdateDTO}.
     *
     * <p>This method applies partial updates to an existing badge entity following
     * HTTP PATCH semantics. Only non-null fields in the DTO will be applied to
     * the target entity, preserving existing values for null fields.</p>
     *
     * <h3>Update Behavior:</h3>
     * <ul>
     *   <li><strong>Null Fields</strong>: Ignored, existing values preserved</li>
     *   <li><strong>Name Updates</strong>: Applied directly to entity</li>
     *   <li><strong>Preserved Fields</strong>: ID, user, sortOrder, labelIds, labelOrder</li>
     * </ul>
     *
     * <h3>Usage Note:</h3>
     * <p>This method modifies the target entity in-place and should be called
     * within a transactional context to ensure data consistency.</p>
     *
     * @param dto the update data containing new values
     * @param entity the existing badge entity to be updated
     */
    void updateEntityFromDto(BadgeUpdateDTO dto, @MappingTarget Badge entity);

    @Mapping(target = "id", source = "badge.id")
    @Mapping(target = "name", source = "badge.name")
    @Mapping(target = "sortOrder", source = "badge.sortOrder")
    @Mapping(target = "timeStats", source = "stats")
    BadgeResponseDTO toResponseDTO(Badge badge, TimeStatsDTO stats);

    /**
     * Creates a comprehensive {@link BadgeResponseDTO} with resolved label information.
     *
     * <p>This enhanced version of the response DTO mapping includes fully resolved
     * label information with color data, providing complete badge information for
     * client applications. It combines the basic badge data with time statistics
     * and associated label details.</p>
     *
     * <h3>Label Resolution:</h3>
     * <ul>
     *   <li><strong>Color Information</strong>: Each label includes color from predefined palette</li>
     *   <li><strong>Complete Data</strong>: ID, name, and visual design information</li>
     *   <li><strong>Null Safety</strong>: Gracefully handles null label sets</li>
     *   <li><strong>UI Ready</strong>: Provides all data needed for badge visualization</li>
     * </ul>
     *
     * <h3>Usage Context:</h3>
     * <p>This method is typically used when the service layer has already resolved
     * the badge's associated labels through {@code LabelService.getLabelsByIds()},
     * providing a complete badge representation for client consumption.</p>
     *
     * @param badge the badge entity containing basic information
     * @param stats computed time statistics for the badge
     * @param labels resolved label information with color data
     * @return complete badge response DTO with all associated data
     */
    default BadgeResponseDTO toResponseDTO(Badge badge, TimeStatsDTO stats, Set<BadgeLabelDTO> labels) {
        BadgeResponseDTO dto = toResponseDTO(badge, stats);
        return new BadgeResponseDTO(
                dto.id(),
                dto.name(),
                dto.sortOrder(),
                dto.timeStats(),
                labels != null ? labels : Set.of()
        );
    }
}

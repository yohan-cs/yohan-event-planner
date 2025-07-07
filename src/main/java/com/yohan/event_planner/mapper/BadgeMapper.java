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

    Badge toEntity(BadgeCreateDTO dto);

    void updateEntityFromDto(BadgeUpdateDTO dto, @MappingTarget Badge entity);

    @Mapping(target = "id", source = "badge.id")
    @Mapping(target = "name", source = "badge.name")
    @Mapping(target = "sortOrder", source = "badge.sortOrder")
    @Mapping(target = "timeStats", source = "stats")
    BadgeResponseDTO toResponseDTO(Badge badge, TimeStatsDTO stats);

    /**
     * Optional helper if you want to combine resolved labels into the response DTO.
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

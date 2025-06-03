package com.yohan.event_planner.mapper;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface for converting between {@link Event} entities and Data Transfer Objects (DTOs).
 *
 * <p>
 * Uses MapStruct to generate implementation for mapping:
 * <ul>
 *   <li>Creating {@link Event} entities from {@link EventCreateDTO}.</li>
 *   <li>Updating existing {@link Event} entities from {@link EventUpdateDTO} with null properties ignored.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The {@link EventResponseDTO} mapping is handled manually in {@link com.yohan.event_planner.service.EventServiceImpl}
 * because it requires viewer-specific timezone context and conditional formatting.
 * </p>
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EventMapper {

    /**
     * Singleton instance for non-Spring usage.
     */
    EventMapper INSTANCE = Mappers.getMapper(EventMapper.class);

    /**
     * Converts an {@link EventCreateDTO} to an {@link Event} entity.
     * <p>
     * This method assumes that the {@link EventCreateDTO} contains all necessary fields to create a new event.
     * For example, the {@link EventCreateDTO} should contain the start and end times, and any relevant event details.
     * </p>
     *
     * @param dto the DTO containing event creation data
     * @return a new {@link Event} entity
     */
    Event toEntity(EventCreateDTO dto);

    /**
     * Updates an existing {@link Event} entity using values from {@link EventUpdateDTO}.
     * <p>
     * Null values in the {@link EventUpdateDTO} are ignored, and only non-null fields will be applied to the existing {@link Event} entity.
     * This method allows partial updates to an event.
     * </p>
     *
     * @param dto    the DTO containing partial update data
     * @param entity the existing {@link Event} entity to update
     */
    void updateEntityFromDto(EventUpdateDTO dto, @MappingTarget Event entity);
}

package com.yohan.event_planner.mapper;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Mapper interface for converting between {@link Event} entities and Data Transfer Objects (DTOs}.
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

    EventMapper INSTANCE = Mappers.getMapper(EventMapper.class);

    Event toEntity(EventCreateDTO dto);

    void updateEntityFromDto(EventUpdateDTO dto, @MappingTarget Event entity);

    // === Optional field adapters for MapStruct ===

    default ZonedDateTime mapOptionalZonedDateTime(Optional<ZonedDateTime> optional) {
        return optional != null ? optional.orElse(null) : null;
    }

    default String mapOptionalString(Optional<String> optional) {
        return optional != null ? optional.orElse(null) : null;
    }
}

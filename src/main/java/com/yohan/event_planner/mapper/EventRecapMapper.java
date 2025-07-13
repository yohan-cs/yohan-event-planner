package com.yohan.event_planner.mapper;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.EventRecap;
import com.yohan.event_planner.domain.RecapMedia;
import com.yohan.event_planner.dto.EventRecapResponseDTO;
import com.yohan.event_planner.dto.RecapMediaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link EventRecap} domain entities and related DTOs.
 *
 * <p>This mapper specializes in creating flat response DTOs that combine data from multiple
 * related entities (EventRecap, Event, and RecapMedia) into a single, client-friendly structure.
 * The flat DTO approach simplifies client-side data handling by eliminating the need for
 * nested object navigation.</p>
 *
 * <h3>Supported Transformations:</h3>
 * <ul>
 *     <li>Converting {@link EventRecap} entities to {@link EventRecapResponseDTO} for API responses</li>
 *     <li>Flattening related entity data into a single response structure</li>
 *     <li>Including associated media items through {@link RecapMediaMapper} integration</li>
 * </ul>
 *
 * <h3>Flat DTO Structure:</h3>
 * <p>The response DTO flattens hierarchical data into a single level:</p>
 * <ul>
 *   <li><strong>Event Data</strong>: Name, date, duration, label information</li>
 *   <li><strong>Recap Data</strong>: Notes, confirmation status, creator information</li>
 *   <li><strong>Media Data</strong>: Associated media items with full details</li>
 * </ul>
 *
 * <h3>Null Handling:</h3>
 * <p>Uses {@code NullValuePropertyMappingStrategy.IGNORE} to skip null fields during mapping,
 * ensuring clean response structures without null values where appropriate.</p>
 */
@Mapper(
        componentModel = "spring",
        uses = RecapMediaMapper.class, // include RecapMediaMapper
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EventRecapMapper {

    /**
     * Converts a {@link EventRecap} and its associated {@link Event} into a flat {@link EventRecapResponseDTO}.
     *
     * @param recap the event recap
     * @param event the event associated with the recap
     * @param media the list of media items associated with the recap
     * @return the recap response DTO
     */
    @Mapping(target = "id", source = "recap.id")
    @Mapping(target = "eventName", source = "event.name")
    @Mapping(target = "username", source = "recap.creator.username")
    @Mapping(target = "date", source = "event.startTime")
    @Mapping(target = "durationMinutes", source = "event.durationMinutes")
    @Mapping(target = "labelName", source = "event.label.name")
    @Mapping(target = "notes", source = "recap.notes")
    @Mapping(target = "media", source = "media")
    @Mapping(target = "unconfirmed", source = "recap.unconfirmed") // ✅ Added mapping for unconfirmed field
    EventRecapResponseDTO toResponseDTO(EventRecap recap, Event event, List<RecapMediaResponseDTO> media);
}

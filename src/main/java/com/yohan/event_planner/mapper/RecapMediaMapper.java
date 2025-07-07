package com.yohan.event_planner.mapper;

import com.yohan.event_planner.domain.RecapMedia;
import com.yohan.event_planner.dto.RecapMediaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between {@link RecapMedia} domain entities and related DTOs.
 *
 * <p>
 * This mapper supports the following transformations:
 * <ul>
 *     <li>Converting {@link RecapMedia} entities to {@link RecapMediaResponseDTO} for API responses</li>
 * </ul>
 * </p>
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface RecapMediaMapper {

    /**
     * Converts a {@link RecapMedia} entity into a {@link RecapMediaResponseDTO}.
     *
     * @param media the recap media entity
     * @return the media response DTO
     */
    @Mapping(target = "id", source = "media.id")
    @Mapping(target = "mediaUrl", source = "media.mediaUrl")
    @Mapping(target = "mediaType", source = "media.mediaType")
    @Mapping(target = "durationSeconds", source = "media.durationSeconds")
    @Mapping(target = "mediaOrder", source = "media.mediaOrder")
    RecapMediaResponseDTO toResponseDTO(RecapMedia media);

}

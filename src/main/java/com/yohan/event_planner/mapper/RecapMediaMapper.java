package com.yohan.event_planner.mapper;

import com.yohan.event_planner.domain.RecapMedia;
import com.yohan.event_planner.dto.RecapMediaResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between {@link RecapMedia} domain entities and related DTOs.
 *
 * <p>This mapper handles the conversion of media attachment entities to client-facing DTOs,
 * focusing on preserving media metadata and ordering information for proper client-side
 * rendering and presentation.</p>
 *
 * <h3>Supported Transformations:</h3>
 * <ul>
 *     <li>Converting {@link RecapMedia} entities to {@link RecapMediaResponseDTO} for API responses</li>
 *     <li>Preserving media type information for client categorization</li>
 *     <li>Maintaining media ordering for sequential presentation</li>
 * </ul>
 *
 * <h3>Enum Mapping:</h3>
 * <p>The mapper automatically handles {@code RecapMediaType} enum conversion between
 * domain entities and DTOs, supporting the following media types:</p>
 * <ul>
 *   <li><strong>IMAGE</strong>: Static image files (jpg, png, gif, etc.)</li>
 *   <li><strong>VIDEO</strong>: Video files with duration tracking</li>
 *   <li><strong>AUDIO</strong>: Audio files with duration information</li>
 * </ul>
 *
 * <h3>Null Handling:</h3>
 * <p>Uses {@code NullValuePropertyMappingStrategy.IGNORE} to ensure clean response
 * structures, particularly useful for optional fields like duration for image media.</p>
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

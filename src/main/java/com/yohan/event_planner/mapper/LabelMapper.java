package com.yohan.event_planner.mapper;

import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.dto.LabelCreateDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.LabelUpdateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between {@link Label} domain entities and related DTOs.
 *
 * <p>
 * This mapper supports the following transformations:
 * <ul>
 *     <li>Creating new {@link Label} entities from {@link LabelCreateDTO}</li>
 *     <li>Applying partial updates using {@link LabelUpdateDTO}</li>
 *     <li>Converting {@link Label} entities to {@link LabelResponseDTO}</li>
 * </ul>
 * </p>
 *
 * <p><strong>Null handling:</strong>
 * Fields in {@link LabelUpdateDTO} that are {@code null} will be ignored during patch operations.
 * </p>
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface LabelMapper {

    /**
     * Converts a {@link LabelCreateDTO} to a {@link Label} entity.
     *
     * <p>
     * Caller must manually assign the creator before persisting the entity.
     * </p>
     *
     * @param dto the label creation data
     * @return a new {@link Label} entity
     */
    Label toEntity(LabelCreateDTO dto);

    /**
     * Applies non-null fields from {@link LabelUpdateDTO} to an existing {@link Label} entity.
     *
     * @param dto the update patch
     * @param entity the entity to update
     */
    void updateEntityFromDto(LabelUpdateDTO dto, @MappingTarget Label entity);

    /**
     * Converts a {@link Label} entity to a {@link LabelResponseDTO} for client-facing views.
     *
     * @param label the source entity
     * @return the response DTO
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "creatorUsername", source = "creator.username")
    LabelResponseDTO toResponseDTO(Label label);
}

package org.springframework.samples.petclinic.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.samples.petclinic.rest.validation.InvalidDtoException;

import java.time.LocalDate;

/**
 * A booking for a vet visit.
 */
@Schema(name = "Visit", description = "A booking for a vet visit.")
@JsonTypeName("Visit")
public record VisitDto(

    @Valid
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(name = "date", example = "2013-01-01", description = "The date of the visit.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("date")
    @Nullable LocalDate date,

    @NotNull @Size(min = VisitDto.DESCRIPTION_MIN_LENGTH, max = VisitDto.DESCRIPTION_MAX_LENGTH)
    @Schema(name = "description", example = "rabies shot", description = "The description for the visit.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("description")
    String description,

    @Min(value = VisitDto.ID_MIN)
    @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, example = "1", description = "The ID of the visit.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    Integer id,

    @NotNull @Min(value = VisitDto.PET_ID_MIN)
    @Schema(name = "petId", example = "1", description = "The ID of the pet.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("petId")
    Integer petId
) {

    public static final int DESCRIPTION_MIN_LENGTH = 1;
    public static final int DESCRIPTION_MAX_LENGTH = 255;
    public static final int ID_MIN = 0;
    public static final int PET_ID_MIN = 0;

    public VisitDto {
        if (description == null) {
            throw new InvalidDtoException("description must not be null");
        }
        if (description.length() < DESCRIPTION_MIN_LENGTH || description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new InvalidDtoException("description length must be between %d and %d but was %d"
                .formatted(DESCRIPTION_MIN_LENGTH, DESCRIPTION_MAX_LENGTH, description.length()));
        }
        if (id != null && id < ID_MIN) {
            throw new InvalidDtoException("id must be at least %d but was %d".formatted(ID_MIN, id));
        }
        if (petId != null && petId < PET_ID_MIN) {
            throw new InvalidDtoException("petId must be at least %d but was %d".formatted(PET_ID_MIN, petId));
        }
    }
}

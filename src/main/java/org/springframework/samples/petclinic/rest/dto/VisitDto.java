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

    @NotNull @Size(min = 1, max = 255)
    @Schema(name = "description", example = "rabies shot", description = "The description for the visit.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("description")
    String description,

    @Min(value = 0)
    @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, example = "1", description = "The ID of the visit.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    Integer id,

    @NotNull @Min(value = 0)
    @Schema(name = "petId", example = "1", description = "The ID of the pet.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("petId")
    Integer petId
) {
}

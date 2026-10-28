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
import org.springframework.samples.petclinic.rest.validation.PetAgeValidation;

import java.time.LocalDate;
import java.util.List;

/**
 * A pet.
 */
@Schema(name = "Pet", description = "A pet.")
@JsonTypeName("Pet")
public record PetDto(

    @NotNull @Size(max = PetDto.NAME_MAX_LENGTH)
    @Schema(name = "name", example = "Leo", description = "The name of the pet.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    String name,

    @PetAgeValidation
    @NotNull @Valid
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(name = "birthDate", example = "2010-09-07", description = "The date of birth of the pet.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("birthDate")
    LocalDate birthDate,

    @NotNull @Valid
    @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("type")
    PetTypeDto type,

    @Min(value = PetDto.ID_MIN)
    @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, example = "1", description = "The ID of the pet.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    Integer id,

    @Min(value = PetDto.OWNER_ID_MIN)
    @Schema(name = "ownerId", accessMode = Schema.AccessMode.READ_ONLY, example = "1", description = "The ID of the pet's owner.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("ownerId")
    @Nullable Integer ownerId,

    @Valid
    @Schema(name = "visits", accessMode = Schema.AccessMode.READ_ONLY, description = "Vet visit bookings for this pet.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("visits")
    List<VisitDto> visits
) {

    public static final int NAME_MAX_LENGTH = 30;
    public static final int ID_MIN = 0;
    public static final int OWNER_ID_MIN = 0;

    public PetDto {
        if (name == null) {
            throw new InvalidDtoException("name must not be null");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new InvalidDtoException("name length must be at most %d but was %d".formatted(NAME_MAX_LENGTH, name.length()));
        }
        if (id != null && id < ID_MIN) {
            throw new InvalidDtoException("id must be at least %d but was %d".formatted(ID_MIN, id));
        }
        if (ownerId != null && ownerId < OWNER_ID_MIN) {
            throw new InvalidDtoException("ownerId must be at least %d but was %d".formatted(OWNER_ID_MIN, ownerId));
        }
        visits = visits == null ? List.of() : List.copyOf(visits);
    }
}

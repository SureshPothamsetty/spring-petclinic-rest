package org.springframework.samples.petclinic.rest.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.rest.validation.InvalidDtoException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PetDtoTests {

    private static final PetTypeDto DOG = new PetTypeDto().id(2).name("dog");

    private static PetDto pet(String name, Integer id, Integer ownerId) {
        return new PetDto(name, LocalDate.now(), DOG, id, ownerId, List.of());
    }

    @Test
    void nameNullThrows() {
        assertThrows(InvalidDtoException.class, () -> pet(null, 1, 1));
    }

    @Test
    void nameOf31CharsThrows() {
        assertThrows(InvalidDtoException.class, () -> pet("x".repeat(31), 1, 1));
    }

    @Test
    void nameOf30CharsSucceeds() {
        assertDoesNotThrow(() -> pet("x".repeat(30), 1, 1));
    }

    @Test
    void emptyNameSucceeds() {
        assertDoesNotThrow(() -> pet("", 1, 1));
    }

    @Test
    void negativeIdThrows() {
        assertThrows(InvalidDtoException.class, () -> pet("Leo", -1, 1));
    }

    @Test
    void negativeOwnerIdThrows() {
        assertThrows(InvalidDtoException.class, () -> pet("Leo", 1, -1));
    }

    @Test
    void nullOwnerIdAndNullIdSucceed() {
        assertDoesNotThrow(() -> pet("Leo", null, null));
    }

    @Test
    void nullVisitsBecomeEmptyList() {
        PetDto dto = new PetDto("Leo", LocalDate.now(), DOG, 1, 1, null);
        assertThat(dto.visits()).isEmpty();
    }

    @Test
    void visitsAreImmutable() {
        PetDto dto = new PetDto("Leo", LocalDate.now(), DOG, 1, 1, null);
        VisitDto visit = new VisitDto(null, "rabies shot", 1, 1);
        assertThrows(UnsupportedOperationException.class, () -> dto.visits().add(visit));
    }

    @Test
    void visitsAreDefensivelyCopied() {
        List<VisitDto> source = new ArrayList<>();
        PetDto dto = new PetDto("Leo", LocalDate.now(), DOG, 1, 1, source);
        source.add(new VisitDto(null, "rabies shot", 1, 1));
        assertThat(dto.visits()).isEmpty();
    }

    @Test
    void birthDateInTheFutureReportsViolationOnBirthDate() {
        PetDto dto = new PetDto("Leo", LocalDate.now().plusDays(1), DOG, 1, 1, List.of());
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<PetDto>> violations = validator.validate(dto);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains("birthDate");
        }
    }

    @Test
    void jsonWithoutIdAndVisitsDeserializes() {
        JsonMapper mapper = JsonMapper.builder().build();
        String json = "{\"name\":\"Leo\",\"birthDate\":\"2020-01-01\",\"type\":{\"id\":2,\"name\":\"dog\"}}";
        PetDto dto = mapper.readValue(json, PetDto.class);
        assertThat(dto.id()).isNull();
        assertThat(dto.visits()).isEmpty();
        assertThat(dto.name()).isEqualTo("Leo");
    }
}

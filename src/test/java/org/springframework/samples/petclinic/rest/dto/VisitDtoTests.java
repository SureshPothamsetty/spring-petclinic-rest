package org.springframework.samples.petclinic.rest.dto;

import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.rest.validation.InvalidDtoException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VisitDtoTests {

    private static VisitDto visit(String description, Integer id, Integer petId) {
        return new VisitDto(LocalDate.now(), description, id, petId);
    }

    @Test
    void descriptionNullThrows() {
        assertThrows(InvalidDtoException.class, () -> visit(null, 1, 1));
    }

    @Test
    void descriptionEmptyThrows() {
        assertThrows(InvalidDtoException.class, () -> visit("", 1, 1));
    }

    @Test
    void descriptionOf256CharsThrows() {
        assertThrows(IllegalArgumentException.class, () -> visit("x".repeat(256), 1, 1));
    }

    @Test
    void descriptionOf1CharSucceeds() {
        assertDoesNotThrow(() -> visit("x", 1, 1));
    }

    @Test
    void descriptionOf255CharsSucceeds() {
        assertDoesNotThrow(() -> visit("x".repeat(255), 1, 1));
    }

    @Test
    void negativeIdThrows() {
        assertThrows(InvalidDtoException.class, () -> visit("rabies shot", -1, 1));
    }

    @Test
    void negativePetIdThrows() {
        assertThrows(InvalidDtoException.class, () -> visit("rabies shot", 1, -1));
    }

    @Test
    void zeroIdAndPetIdSucceed() {
        assertDoesNotThrow(() -> visit("rabies shot", 0, 0));
    }

    @Test
    void nullIdAndNullDateSucceed() {
        assertDoesNotThrow(() -> new VisitDto(null, "rabies shot", null, 1));
    }

    @Test
    void jsonWithoutIdDeserializes() {
        JsonMapper mapper = JsonMapper.builder().build();
        VisitDto dto = mapper.readValue("{\"description\":\"rabies shot\",\"petId\":3}", VisitDto.class);
        assertThat(dto.id()).isNull();
        assertThat(dto.petId()).isEqualTo(3);
        assertThat(dto.description()).isEqualTo("rabies shot");
    }
}

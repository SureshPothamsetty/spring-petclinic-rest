package org.springframework.samples.petclinic.rest.dto;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fails when the constraints in {@code openapi.yml} and the constants of the hand-written records differ.
 */
class OpenApiConstraintDriftTests {

    private static Map<String, Object> schemas;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void loadSpec() throws IOException {
        try (InputStream in = OpenApiConstraintDriftTests.class.getResourceAsStream("/openapi.yml")) {
            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> components = (Map<String, Object>) root.get("components");
            schemas = (Map<String, Object>) components.get("schemas");
        }
    }

    /** Collects the properties of a schema, following {@code allOf} members and local {@code $ref}s. */
    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> properties(String schemaName) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        collect((Map<String, Object>) schemas.get(schemaName), result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void collect(Map<String, Object> schema, Map<String, Map<String, Object>> into) {
        String ref = (String) schema.get("$ref");
        if (ref != null) {
            collect((Map<String, Object>) schemas.get(ref.substring(ref.lastIndexOf('/') + 1)), into);
        }
        List<Object> allOf = (List<Object>) schema.get("allOf");
        if (allOf != null) {
            for (Object member : allOf) {
                collect((Map<String, Object>) member, into);
            }
        }
        Map<String, Map<String, Object>> props = (Map<String, Map<String, Object>>) schema.get("properties");
        if (props != null) {
            into.putAll(props);
        }
    }

    @Test
    void visitConstraintsMatchRecordConstants() {
        Map<String, Map<String, Object>> visit = properties("Visit");
        assertThat(visit.get("description")).containsEntry("minLength", VisitDto.DESCRIPTION_MIN_LENGTH);
        assertThat(visit.get("description")).containsEntry("maxLength", VisitDto.DESCRIPTION_MAX_LENGTH);
        assertThat(visit.get("id")).containsEntry("minimum", VisitDto.ID_MIN);
        assertThat(visit.get("petId")).containsEntry("minimum", VisitDto.PET_ID_MIN);
    }

    @Test
    void petConstraintsMatchRecordConstants() {
        Map<String, Map<String, Object>> pet = properties("Pet");
        assertThat(pet.get("name")).containsEntry("maxLength", PetDto.NAME_MAX_LENGTH);
        assertThat(pet.get("id")).containsEntry("minimum", PetDto.ID_MIN);
        assertThat(pet.get("ownerId")).containsEntry("minimum", PetDto.OWNER_ID_MIN);
    }
}

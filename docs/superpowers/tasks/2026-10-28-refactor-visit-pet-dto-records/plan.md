# VisitDto / PetDto Records Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the generated `VisitDto` and `PetDto` with hand-written Java records whose compact constructors validate null and boundary values, with invalid input still returning HTTP 400 and a ProblemDetail.

**Architecture:** Records live in `src/main/java/org/springframework/samples/petclinic/rest/dto/` (same package as the generated DTOs, so existing imports stay unchanged). `schemaMappings` in the openapi-generator plugin suppresses generation of `Pet` and `Visit`. All other DTOs stay generated. Commit per task using the repository's existing convention.

**Tech Stack:** Java 17 (Spring Boot 4.1.1 parent), openapi-generator-maven-plugin 7.25.0, MapStruct 1.6.3, Jackson 3 (`tools.jackson`), JUnit 5 and MockMvc.

## Acceptance criteria

- `VisitDto` and `PetDto` are records in `src/main/java`; `target/generated-sources` no longer contains them, while `PetFieldsDto`, `VisitFieldsDto`, `OwnerDto`, `VetDto`, `PetPageDto` and the other DTOs are still generated and compile (including `Pet.visits` referencing Visit).
- Build stays on Java 17. This deliberately departs from the "Java 21" wording of the request. Records and compact constructors work on 17.
- No global custom mustache template is used.
- Visit: `description` is required with 1 to 255 characters; `id` and `petId` are at least 0 when present; `date` and `id` may be null (id is readOnly and is absent on create).
- Pet: `name` is required with at most 30 characters (empty string stays valid, as today); `id` and `ownerId` are at least 0 when present; `ownerId` may be null; `birthDate` is still checked by `PetAgeValidation`; `visits` null becomes an immutable empty list.
- Violations throw `InvalidDtoException` (an `IllegalArgumentException`). Bad request bodies and mapper failures return 400 with a ProblemDetail, never the catch-all 500.
- JSON property names, `@Schema` metadata and the `@NotNull`/`@Size`/`@Min`/`@Valid` annotations (springdoc output) are preserved.
- A test fails if the `openapi.yml` constraints and the record constants differ.
- Existing tests are migrated to the record API and pass. New tests cover boundaries, nulls and the 400 behaviour.

## Global Constraints

- Do not bump `java.version`. Do not add a `templateDirectory` or custom `pojo.mustache`.
- Do not hand-edit `target/generated-sources`.
- `openapi.yml` constraint values: `Visit.description` min 1, max 255; `Pet.name` max 30; `id`/`petId`/`ownerId` minimum 0.
- The three declarations (`openapi.yml`, annotations, constructor checks) must use one set of constants in the records. The drift test checks `openapi.yml` against those constants.

## Review Focus

- `POST /api/visits` with no `id` returns 201 and is not rejected by the constructor (Task 3 and Task 4 tests).
- A JSON body with an unparseable date or malformed JSON returns 400, not 500 (Task 4).
- A JSON Pet body with no `visits` yields an empty immutable list (Task 3).
- `PUT /api/pets/{id}` with a birthDate rejected by `PetAgeValidator` still returns 400 (Task 3 and Task 4).
- A `Visit` entity with a null description mapped to `VisitDto` returns 400 on GET rather than 500, per the user's decision on mapper failures (Task 4).

---

### Task 1: De-risk schemaMappings and move main code to records

**Files:**
- Modify: `pom.xml:369-371` (add `<schemaMappings>` for `Pet` and `Visit` next to `importMappings`)
- Create: `src/main/java/org/springframework/samples/petclinic/rest/dto/VisitDto.java`
- Create: `src/main/java/org/springframework/samples/petclinic/rest/dto/PetDto.java`
- Modify: `src/main/java/org/springframework/samples/petclinic/rest/controller/v1/PetRestControllerV1.java:80-82` (`getX()` to `x()`)
- Check, and edit only if compile requires: `mapper/PetMapper.java`, `mapper/VisitMapper.java`, `OwnerRestControllerV1.java`, `VisitRestControllerV1.java`

**Interfaces:**
- Produces: `record VisitDto(LocalDate date, String description, Integer id, Integer petId)` and `record PetDto(String name, LocalDate birthDate, PetTypeDto type, Integer id, Integer ownerId, List<VisitDto> visits)`. The component order matches the old generated field order. Task 3 adds validation to these.

**Test-first: no — build-level proof of the generator route. Before the change, `target/generated-sources/.../dto/PetDto.java` exists and the hand-written records would collide with it. The pass condition is the generation and compile checks below.**

- [ ] **Step 1:** Add `schemaMappings` to the plugin config: `Pet=org.springframework.samples.petclinic.rest.dto.PetDto` and `Visit=org.springframework.samples.petclinic.rest.dto.VisitDto`. If the generated APIs or `OwnerDto` still do not import the hand-written types, also add matching `importMappings` for `Pet` and `Visit`. If neither variant works, STOP and report. Do not fall back to a mustache template.
- [ ] **Step 2:** Create both records carrying the generated annotations (read the current files in `target/generated-sources/openapi/.../dto/` first): `@JsonTypeName`, `@JsonProperty` per component, `@Schema` descriptions and readOnly flags, `@NotNull @Size @Min @Valid`, `@DateTimeFormat` on `date`, `@Nullable` on `date` and `ownerId`, and `@PetAgeValidation` on `birthDate`. `PetDto.visits` is `@Valid`. No constructor logic yet.
- [ ] **Step 3:** Run `./mvnw -q clean generate-sources`. Expected: `PetDto.java` and `VisitDto.java` are absent from `target/generated-sources`; `PetFieldsDto`, `VisitFieldsDto`, `OwnerDto`, `VetDto`, `PetTypeDto`, `PetPageDto` and `OwnerPageDto` are present; the generated `*Api` and `OwnerDto` import the hand-written types.
- [ ] **Step 4:** Fix the main code. Run `./mvnw -q -DskipTests compile` (test sources are not compiled here). Expected: success. Open the generated `PetMapperImpl` and `VisitMapperImpl` under `target/generated-sources/annotations` and confirm they build the records through the canonical constructor and that `toPetPageDto` is untouched.

### Task 2: Migrate existing tests to the record API

**Files:**
- Modify: `src/test/java/org/springframework/samples/petclinic/rest/controller/{OwnerRestControllerV1Tests,PetRestControllerV1Tests,VisitRestControllerV1Tests,V2RestControllersTests}.java`, plus any other test found by `Grep "new PetDto\(|new VisitDto\(|PetDto|VisitDto" src/test`

**Test-first: no — mechanical migration. The existing suite is the check, and it fails to compile until this task is done.**

- [ ] **Step 1:** Replace each `new PetDto()`/`new VisitDto()` plus setter or fluent-call sequence with the canonical constructor (call sites: `OwnerRestControllerV1Tests:113,119,126,133,143,149`, `PetRestControllerV1Tests:93,99`, `V2RestControllersTests:83,89`). Replace `getX()` reads on these types with `x()`.
- [ ] **Step 2:** Rewrite the error-path tests that build an invalid DTO through setters or the mapper, because Task 3 makes those constructions throw. `VisitRestControllerV1Tests.testCreateVisitError` (~L176) and `testUpdateVisitError` (~L210) should POST or PUT a hand-written JSON string (or `VisitFieldsDto` for the update) with the description missing or null. `PetRestControllerV1Tests` ~L158-210 (null name at ~L182) should serialize a `PetFieldsDto` or a JSON string instead of an invalid `PetDto`. Expected status stays 400.
- [ ] **Step 3:** Run `./mvnw -q test -Dtest='OwnerRestControllerV1Tests,PetRestControllerV1Tests,VisitRestControllerV1Tests,V2RestControllersTests'`. Expected: PASS. The 400 cases are still satisfied by Bean Validation at this point.

### Task 3: Compact constructors, constants and drift test

**Files:**
- Create: `src/main/java/org/springframework/samples/petclinic/rest/validation/InvalidDtoException.java` (`extends IllegalArgumentException`)
- Modify: `rest/dto/VisitDto.java`, `rest/dto/PetDto.java`
- Test: `src/test/java/org/springframework/samples/petclinic/rest/dto/VisitDtoTests.java`, `PetDtoTests.java`, `OpenApiConstraintDriftTests.java`

**Interfaces:**
- Produces: `InvalidDtoException(String message)`. `public static final int` constants `VisitDto.DESCRIPTION_MIN_LENGTH` (1), `DESCRIPTION_MAX_LENGTH` (255), `ID_MIN` (0), `PetDto.NAME_MAX_LENGTH` (30), `ID_MIN` (0), `OWNER_ID_MIN` (0). The annotation attributes (`@Size`, `@Min`) reference these constants.

**Test-first: yes — `VisitDtoTests.descriptionOf256CharsThrows` fails today because the record accepts any value.**

- [ ] **Step 1:** Write the failing tests, all asserting `InvalidDtoException` (via `assertThrows(IllegalArgumentException.class, ...)`) or successful construction:
  - Visit: description null, "" and 256 chars throw; 1 char and 255 chars succeed; `id` -1 and `petId` -1 throw; `id` null and `date` null succeed.
  - Pet: name null and 31 chars throw; 30 chars and "" succeed; `id` -1 and `ownerId` -1 throw; `ownerId` null and `id` null succeed.
  - `PetDto.visits` null gives `visits().isEmpty()`; `visits().add(...)` throws `UnsupportedOperationException`; mutating the caller's source list afterwards does not change the record.
  - A Bean Validation `Validator` run on a `PetDto` with a birthDate that `PetAgeValidator` rejects (read the validator for the value) reports a violation on `birthDate`.
  - Jackson deserialization (`tools.jackson` `JsonMapper`) of Pet JSON with no `id` and no `visits` succeeds; Visit JSON with no `id` succeeds.
  - Drift: load `src/main/resources/openapi.yml` with SnakeYAML (confirm it is on the test classpath; otherwise use Jackson YAML). For `Visit`/`VisitFields` and `Pet`/`PetFields` (the constraints sit in `allOf` members) assert `description.minLength/maxLength`, `name.maxLength` and `id`/`petId`/`ownerId` `minimum` equal the constants.
- [ ] **Step 2:** Run `./mvnw -q test -Dtest='VisitDtoTests,PetDtoTests,OpenApiConstraintDriftTests'`. Expected: FAIL (the boundary and visits tests).
- [ ] **Step 3:** Add the constants and the compact constructors. Throw `InvalidDtoException` with a message naming the field and limit. Do not null-check `date`, `id`, `ownerId`, `birthDate`, `type` or `petId` (the `@NotNull` annotations keep covering the last three); apply the minimum checks only when the value is non-null. In `PetDto` set `visits = visits == null ? List.of() : List.copyOf(visits)`.
- [ ] **Step 4:** Re-run the command from Step 2. Expected: PASS. Then run the Task 2 test classes; only error-path expectations that depend on Task 4 may fail, and these are fixed there.

### Task 4: Keep invalid input at HTTP 400

**Files:**
- Modify: `src/main/java/org/springframework/samples/petclinic/rest/advice/ExceptionControllerAdvice.java` (add handlers next to `handleMethodArgumentNotValidException`, ~L117; the catch-all is at L80)
- Test: `VisitRestControllerV1Tests.java`, `PetRestControllerV1Tests.java`

**Interfaces:**
- Consumes: `InvalidDtoException` from Task 3.
- Produces: `@ExceptionHandler({InvalidDtoException.class, HttpMessageNotReadableException.class}) ResponseEntity<ProblemDetail> handleInvalidDto(Exception e, HttpServletRequest request)` returning 400. It uses `detailBuild(e, BAD_REQUEST, request.getRequestURL(), ERROR_INVALID_REQUEST)` and puts the root-cause message into `schemaValidationErrors` as a `ValidationMessageDto`. Log at warn, not error.

**Test-first: yes — `VisitRestControllerV1Tests.testCreateVisitDescriptionTooLongReturns400` posts a 256-character description. Today the constructor error falls into the catch-all and returns 500.**

- [ ] **Step 1:** Add MockMvc tests asserting status 400 and a ProblemDetail body (`status` 400, non-empty `schemaValidationErrors`):
  - `POST /api/visits` with a 256-character description.
  - `POST /api/visits` with a null description.
  - `POST /api/visits` with `petId` -1.
  - `POST /api/visits` with no `id` and a valid body returns 201.
  - `POST /api/visits` with a malformed `date` string and with truncated JSON.
  - `PUT /api/pets/{id}` with a 31-character name.
  - `PUT /api/pets/{id}` with a birthDate rejected by `PetAgeValidator`.
  - `GET /api/visits/{id}` where the mocked `ClinicService` returns a `Visit` with a null description (mapper failure) returns 400.
- [ ] **Step 2:** Run `./mvnw -q test -Dtest='VisitRestControllerV1Tests,PetRestControllerV1Tests'`. Expected: FAIL with 500 for the constructor and mapper cases.
- [ ] **Step 3:** Implement `handleInvalidDto` as specified. It must be more specific than the `Exception.class` handler, so no ordering change is needed.
- [ ] **Step 4:** Re-run Step 2's command, then `./mvnw -q test -Dtest='OwnerRestControllerV1Tests,V2RestControllersTests'`. Expected: PASS.

---

## Negative-constraint check

- No global mustache template: Task 1 Step 1 stops and reports rather than falling back to one. No task adds `templateDirectory`.
- No Java 21 bump: no task touches `java.version`.
- `date`, `ownerId` and a create-time `id` stay nullable: Task 3 Step 3 has no null checks on them, and the Task 3 and Task 4 tests pin this.
- 400 instead of 500: Task 4.
- Other DTOs stay generated: Task 1 Step 3 verifies it.
- Non-goals: not stated beyond the above.

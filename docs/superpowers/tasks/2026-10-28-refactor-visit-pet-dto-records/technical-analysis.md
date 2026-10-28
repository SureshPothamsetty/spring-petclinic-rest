# Technical Research

**Task**: dto openapi mapstruct records
**Generated**: 2026-10-28
**Research path**: filesystem

---

## 1. Original Context

Refactor VisitDto and PetDto into Java 21 records with compact constructors for null and boundary validation

(Orchestrator hint, verified: VisitDto and PetDto have no source files; they are generated. See Section 2.)

---

## 2. Codebase Findings

### Existing Implementations
- **No hand-written `VisitDto`/`PetDto` exists** (Glob `**/*{VisitDto,PetDto}*` finds nothing in the repo). They are produced by `openapi-generator-maven-plugin` 7.25.0 into `target/generated-sources/openapi/src/main/java/org/springframework/samples/petclinic/rest/dto/`, added as a source root via `build-helper-maven-plugin` (`pom.xml` ~L391-408). I ran `./mvnw -o -q generate-sources`, which succeeded offline and created `target/` (it did not exist before). The generated classes are `PetDto`, `PetFieldsDto`, `VisitDto`, `VisitFieldsDto` and others.
- Generator config (`pom.xml` L347-390): `generatorName=spring`, `library=spring-boot`, `modelNameSuffix=Dto`, `modelPackage=...petclinic.rest.dto`, `apiPackage=...rest.api`, `interfaceOnly=true`, `useTags=true`, `performBeanValidation=true`, `useSpringBoot3=true`, `useJakartaEe=true`, `openApiNullable=false`, `serializationLibrary=jackson`, `documentationProvider=springdoc`. The only `importMapping` is `Nullable=org.jspecify.annotations.Nullable`. No `typeMappings`, `schemaMappings`, `templateDirectory` or `additionalModelTypeAnnotations`.
- Generated shape: mutable POJOs. Each has a no-arg constructor, a required-args constructor, fluent `x(...)` methods, getters and setters, and `equals`/`hashCode`/`toString`. Each carries `@Schema`, `@JsonTypeName`, `@JsonProperty`, and `@Generated`.
  - `VisitDto` fields: `@Nullable LocalDate date`, `String description`, `Integer id`, `Integer petId`.
  - `PetDto` fields: `String name`, `LocalDate birthDate`, `PetTypeDto type`, `Integer id`, `@Nullable Integer ownerId`, `List<VisitDto> visits = new ArrayList<>()`.
  - The `allOf` schemas are flattened. `VisitDto` does not extend `VisitFieldsDto` and `PetDto` does not extend `PetFieldsDto`. There is no inheritance to preserve.
  - `PetDto` and `VisitDto` live in the same package as the `*FieldsDto` classes, which the update and create-with-fields endpoints use.

### Constraints currently declared in `src/main/resources/openapi.yml` (schemas at L2154-2307)
- **Visit** = `VisitFields` + extras:
  - `date`: string, format date, NOT required, nullable. The generated getter has `@Valid`.
  - `description`: required, `minLength 1`, `maxLength 255` (generated `@NotNull @Size(min=1,max=255)`).
  - `id`: required, int32, `minimum 0`, readOnly (the extras block lists `id` and `petId` as required).
  - `petId`: required, int32, `minimum 0`, not readOnly.
- **Pet** = `PetFields` + extras:
  - `name`: required, `maxLength 30`, no minLength (generated `@NotNull @Size(max=30)`). An empty string is currently valid.
  - `birthDate`: required, date, with `x-field-extra-annotation: @...rest.validation.PetAgeValidation` (a custom constraint). It is applied to the field in both `PetDto` and `PetFieldsDto`.
  - `type`: required `$ref PetType`, `@NotNull @Valid`.
  - `id`: required, int32, `minimum 0`, readOnly.
  - `ownerId`: int32, `minimum 0`, readOnly, NOT required (`@Nullable`).
  - `visits`: required array of `Visit`, readOnly, `@Valid`, defaults to an empty list.
- No `pattern` is declared for Pet or Visit. The pattern constraints in the spec belong to Vet and Owner.
- `PetAgeValidation` (`src/main/java/.../rest/validation/PetAgeValidation.java`) has `@Target({FIELD})`, `@Retention(RUNTIME)` and is backed by `PetAgeValidator`. Its violations are built with `buildConstraintViolationWithTemplate`.

### Architecture and Layers Affected
- **Spec and generation layer**: `openapi.yml` and the `pom.xml` plugin config. Anything that changes generated output is made here.
- **Mapper layer**: `.../petclinic/mapper/PetMapper.java` (`@Mapper(uses = VisitMapper.class)`) and `VisitMapper.java` (`@Mapper(uses = PetMapper.class)`). The two mappers reference each other.
  - `PetMapper`: `toPetDto` (`@Mapping owner.id -> ownerId`), `toPetsDto`, `toPets`, `toPet(PetDto)` (`ownerId -> owner.id`), `toPet(PetFieldsDto)`, the PetType methods, and a default `toPetPageDto` that uses setters on `PetPageDto`.
  - `VisitMapper`: `toVisit(VisitDto)` (`petId -> pet.id`), `toVisit(VisitFieldsDto)`, `toVisitDto` (`pet.id -> petId`), `toVisitsDto`.
- **Controller layer**: `rest/controller/v1/{Pet,Visit,Owner}RestControllerV1.java` implement the generated `*Api` interfaces.
  - `VisitRestControllerV1.addVisit(VisitDto)` takes the body, maps it with `visitMapper.toVisit`, and returns `toVisitDto`. `updateVisit(Integer, VisitFieldsDto)` uses `visitDto.getDate()` and `getDescription()` getters.
  - `PetRestControllerV1.updatePet(Integer, PetDto)` takes a `PetDto` body.
  - `OwnerRestControllerV1` returns `PetDto`/`VisitDto`, and takes `PetFieldsDto`/`VisitFieldsDto` as input.
- **Domain layer** (`model.Pet`, `model.Visit`) is mutable JPA/JDBC-style entities with setters. These are the mapper targets in the DTO-to-entity direction.

### Integration Points
- Java version: **the project targets Java 17, not 21.** `pom.xml` sets no `java.version` or `maven.compiler.*` property. It inherits `java.version=17` and `maven.compiler.release=${java.version}` from `spring-boot-starter-parent` 4.1.1 (verified in the local `.m2` parent pom, L14-16). The enforcer rule `requireJavaVersion [${java.version},)` allows any JDK >= 17. The local JDK is 25.0.2. Records are final in Java 16+, so they work on 17. "Java 21 records" is a naming mismatch, not a blocker. A 21 bump is optional and is not required for records.
- The `spring` generator in 7.25.0 **cannot emit records**. `SpringCodegen` and `AbstractJavaCodegen` class constants contain no "record" option. `JavaSpring/pojo.mustache` hardcodes `public class`, with only `x-class-extra-annotation`, `x-field-extra-annotation`, `{{>sealed}}`, and a `generatedConstructorWithRequiredArgs` option. The 7.25.0 plugin does expose `templateDirectory`, `templateResourcePath`, `typeMappings`, `importMappings`, `schemaMappings`, `modelNameMappings`, `additionalProperties` (confirmed in the plugin's `plugin.xml`), so these are the available mechanisms:
  - (a) A custom `pojo.mustache` that emits records for all models. This is a high blast radius, because it would also convert Owner, Vet, PetType and the rest.
  - (b) `schemaMappings` (or `importMappings`/`typeMappings`) of `Visit` and `Pet` to hand-written record classes, which suppresses their generation. The generated code and API interfaces then import the hand-written types. This is the lowest-risk way to scope the change to just these two. Speculative: I did not try a mapping, so how the generator handles a schema that is referenced inside `allOf` (as `Visit` is from `Pet.visits`) is not verified.
  - (c) Hand-written records in a different package, with the spec and generated API left unchanged. This does not work, because the generated `*Api` signatures reference `...rest.dto.PetDto`/`VisitDto`.
- **MapStruct 1.6.3** (`pom.xml` L26; the annotation processor is on `annotationProcessorPaths`, with `defaultComponentModel=spring`) supports records as both source and target. As a source it reads the record accessors (`name()`, not `getName()`). As a target it uses constructor mapping, and with a record's canonical constructor this is the only option. Records need no `@Builder`. Notes for this repo:
  - The `@Mapping(source = "owner.id", target = "ownerId")` style works unchanged.
  - Collection and nested mapping (`visits` -> `List<VisitDto>`) works through the `uses` chain.
  - A compact-constructor failure at map time surfaces as an exception thrown from the generated mapper implementation.
  - `PetMapper.toPetPageDto` uses `PetPageDto` setters, so it is not affected, because `PetPageDto` stays a class.
  - Record accessors are not JavaBean getters, so any code calling `getX()` must change (see usages).

### Patterns and Conventions
- DTOs are never hand-written today. The spec is the single source of truth for API shape, validation and documentation, and `x-field-extra-annotation` is the existing escape hatch for custom validators.
- The mappers are interfaces that use `@Mapper` and `@Mapping`, with no explicit `componentModel`, because it is set globally in the compiler args.
- `jspecify` `@Nullable`/`@NonNull` is used for nullability (the `Nullable` import mapping, and `@NonNull` in `PetMapper`).
- Validation today is Bean Validation (`jakarta.validation` and Hibernate validator annotations) triggered by `@Valid @RequestBody` on the generated API interfaces. `ExceptionControllerAdvice` maps `MethodArgumentNotValidException` to a `ProblemDetail` 400. It also has a catch-all `Exception` handler.

---

## 3. Documentation Findings

### Guides and Architecture Docs
No guides found. `.ai-run/` contains only `sdlc-factory/`, `docs/` has only `superpowers/`, and there is no ADR directory. Conventions are derived from code exploration.

### Architectural Decisions
None recorded. The decision to generate DTOs from `openapi.yml` is implicit in `pom.xml` (the comment "RestController implementation is provided by Petclinic developers" marks `interfaceOnly`).

### Derived Conventions
Spec-first. The generated API interface carries the validation annotations, and controllers implement it. Custom constraints go in `src/main/java/.../rest/validation`.

---

## 4. Testing Landscape

### Existing Coverage
All under `src/test/java/org/springframework/samples/petclinic/`:
- `rest/controller/VisitRestControllerV1Tests.java` (20 DTO-related references), `PetRestControllerV1Tests.java` (9), `OwnerRestControllerV1Tests.java` (49), `V2RestControllersTests.java` (4), plus the PetType, Specialty and Vet controller tests and `service/clinicService/AbstractClinicServiceTests.java`.
- `new PetDto()` is called at `OwnerRestControllerV1Tests:113,119,143`, `PetRestControllerV1Tests:93,99`, and `V2RestControllersTests:83,89`. `new VisitDto()` is called at `OwnerRestControllerV1Tests:126,133,149`. All of them then use setters, so each call site breaks when the types become records. The total is about 105 DTO and setter references in 8 test files.
- The error-path tests are the key ones: `VisitRestControllerV1Tests.testCreateVisitError` (L176) sets `description=null` and `id=null` on the entity, then calls `visitMapper.toVisitDto(newVisit)` and POSTs the JSON, expecting **400**. `testUpdateVisitError` (L210) does the same with `description=null`.
  - Both build the invalid DTO through the mapper. If the compact constructor throws on null `description`, the exception is thrown inside `toVisitDto` in the test body, and the test fails before it makes a request.
  - They would need to build the invalid JSON by hand.
- `PetRestControllerV1Tests` L158-210 serialize `PetFieldsDto`/`PetDto` with Jackson `JsonMapper` (`tools.jackson.databind.ObjectMapper`, so Jackson 3), and expect 400 for invalid payloads (e.g. `setName(null)` at L182).

### Testing Framework and Patterns
Spring Boot test stack (JUnit 5), MockMvc standalone with `ExceptionControllerAdvice`, `@MockitoBean ClinicService`, `@WithMockUser(roles="OWNER_ADMIN")`, real MapStruct mappers autowired, and Jackson-serialized request bodies.

### Coverage Gaps
- There are no unit tests for the mappers on their own, and none for `PetAgeValidator` against the DTOs.
- There are no tests for boundary values: `name` of exactly 30 and of 31 characters, `description` of 255 and of 256, and `id` or `petId` of -1.
- There are no tests for what happens when a record constructor throws during JSON deserialization.

---

## 5. Configuration and Environment

### Environment Variables
None relevant found for the DTO or generation path.

### Configuration Files
- `pom.xml` for generator and compiler config (see Section 2). `src/main/resources/openapi.yml` for the schemas. The generator reads `inputSpec` from `${project.basedir}/src/main/resources/openapi.yml`.
- There is no `.mvn` jvm config and no CI config containing a Java version. I did not locate Dockerfile or CI config referencing the DTOs (the `jib` plugin is in `pom.xml`).

### Feature Flags and Deployment Concerns
None. The build depends on `target/generated-sources` being regenerated on every build. The project has no committed copies of the DTOs.

---

## 6. Risk Indicators

- **Premise mismatch (high)**: the DTOs are generated, not source. Editing `target/generated-sources` is lost on the next build. The change has to be done through the generator config, with the spec, a custom template, or `schemaMappings` plus hand-written records.
- **Premise mismatch (medium)**: the target is Java 17, not 21. Either is fine for records, but the task text and the build (`java.version` inherited from the Boot parent) disagree. Needs a decision on whether to bump.
- **Generator has no records support (high)**: 7.25.0 `spring` generator emits only classes. Options (a) and (b) in Section 2 are both custom work. Option (a) converts every model, including Owner, Vet, PetType and Specialty, and `PetPageDto`/`OwnerPageDto`, which `PetMapper.toPetPageDto` builds with setters. Speculative: option (b) may be the narrower route, but it is untested.
- **Spec/code duplication (medium)**: once the types are hand-written records, the constraints in `openapi.yml` (`maxLength 30`, `minLength 1`, `maxLength 255`, `minimum 0`) and the compact constructor can drift apart. The constraints also feed springdoc and `@Schema`, which the hand-written records would need to carry by hand.
- **Validation semantics change (high)**: today a bad body returns 400 through `MethodArgumentNotValidException` and a `ProblemDetail` carrying field errors. Speculative: a compact constructor that throws would fail inside Jackson deserialization and surface as an `HttpMessageNotReadableException`, which the catch-all `@ExceptionHandler(Exception.class)` in `ExceptionControllerAdvice` may map to 500 instead of 400. The handler for that exception type needs checking and probably adding.
- **Nullability contradictions**: `Pet.required` and `Visit.required` mark `id` as required even though it is `readOnly`. For a POST of a new visit or a `PUT /pets/{id}` body, `id` is null or absent. A strict non-null check in the compact constructor would reject creates. The existing `testCreateVisitSuccess` sends `id=999`, but `testCreateVisitError` sends `id=null`. `ownerId` is `@Nullable` and `date` is nullable and must not be null-checked.
- **`PetAgeValidation`**: `@Target(FIELD)` only. On a record component, the annotation propagates to the private field, so it still works. Moving the check into the compact constructor would drop the existing `ConstraintValidator` path, so keep the annotation.
- **Mapper circularity**: `PetMapper` and `VisitMapper` use each other. With constructor-based targets, `PetDto.visits` is non-null with a default `new ArrayList<>()` today. MapStruct passes null for an absent source collection to the constructor, so a null check in the compact constructor could trigger where the class had a default. Defensive copying with `List.copyOf` would also change mutability, and controllers/tests that call `addVisitsItem` or `setVisits` would break.
- **Wide ripple (medium)**: 8 test files, 3 controllers and 2 mappers consume the setters, fluent methods and `new X()`. `VisitRestControllerV1.updateVisit` uses `getDate()`/`getDescription()` on `VisitFieldsDto`, which stays a class.
- **Jackson 3 (`tools.jackson`)**: record support is built in, but `@JsonProperty`/`@JsonTypeName` (`com.fasterxml.jackson.annotation` is still the annotation package for Jackson 3) and the `@DateTimeFormat` on `date` need checking on the record.
- **Docs gap**: no guides or ADRs, so there is no documented policy on "who owns validation".

---

## 7. Summary for Complexity Assessment

`VisitDto` and `PetDto` are not source files. They are mutable POJOs generated from `openapi.yml` by openapi-generator-maven-plugin 7.25.0 (`spring` generator, `spring-boot` library), and the project targets Java 17 (inherited from Spring Boot 4.1.1). The 7.25.0 spring generator has no record output. The change therefore cannot be made by editing Java alone. It needs generator-level work (a custom mustache template or `schemaMappings` to hand-written records) in addition to the Java changes. The layers touched are the build and spec layer (`pom.xml`, `openapi.yml`), the DTO layer, 2 MapStruct mappers, 3 controllers and about 8 test files.

MapStruct 1.6.3 handles records as both source and target through constructor mapping, so the mappers are the lowest-risk part. The larger risk is behavioural. Validation moves from Bean Validation on the generated getters (400 with a `ProblemDetail`) to constructor exceptions that fire during Jackson deserialization and inside mapper calls. Existing error-path tests (`testCreateVisitError`, `testUpdateVisitError`, and the null-name pet tests) build their invalid DTOs through the mapper or setters and will fail for reasons unrelated to the feature.

The declared constraints are modest: `description` is required with 1 to 255 characters; `name` is required with a 30-character maximum; `id`, `petId`, and `ownerId` have a minimum of 0; `birthDate` carries the custom `PetAgeValidation`. Test coverage of the controller paths is decent, but there are no mapper unit tests and no boundary tests. There is no documentation on conventions. Net: moderate size, with a generator-config unknown (the `schemaMappings` route is untried), a Java 17 vs 21 premise mismatch, and an error-handling behaviour change that needs a design decision.

---

## 8. External References

None named by the task.

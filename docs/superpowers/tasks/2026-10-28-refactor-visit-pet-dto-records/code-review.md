---
schema: sdlc-factory/code-review@1
decision: request-changes
reviewed_head: 0020e7b8400a46cc91fe51a5df497959aa2db1d8
---

# Code review — 2026-10-28-refactor-visit-pet-dto-records

**Decision:** request-changes (final round, confidence low)  
**Reviewed head:** `0020e7b` · **Rounds:** final

No spec was supplied (acceptance n/a), so confidence is low and the stored-data 400 question is kept blocking as a patch with its ambiguity stated; all other lenses ran and every candidate was checked against source. 3 blocking findings, 0 deferred, 18 dismissed as noise or already handled; fix pack written to code-review-fix-pack.local.md.

## Open — 3

### CR-001 · code · major — Stored-data mapping failures return 400

`src/main/java/org/springframework/samples/petclinic/rest/advice/ExceptionControllerAdvice.java:162` · not re-checked

**Problem:** The record compact constructors now throw InvalidDtoException, and the mappers (PetMapper.toPetDto, VisitMapper.toVisitDto) build the records on every read path. handleInvalidDto maps that exception to HTTP 400 wherever it is thrown, so a persisted row with a null or out-of-range name or description makes GET /api/visits/{id}, GET /api/pets and the list endpoints return 400 with a validation-error body. testGetVisitWithNullDescriptionReturns400 pins this behaviour.

**Impact:** A server-side data fault is reported as a caller error, and one bad row fails a whole list endpoint. Before this change the DTOs carried no constructor checks, so such rows were serialised. Whether 400 is intended for response-side failures needs human confirmation; no spec exists to settle it, so it stays blocking.

**Recommendation:** Keep 400 only for inbound request deserialization. Return 500 without echoing the message when InvalidDtoException is raised while mapping stored entities to response DTOs, and update testGetVisitWithNullDescriptionReturns400. If 400 is intended, record that decision explicitly.

### CR-002 · code · major — Invalid-payload handler leaks root-cause message

`src/main/java/org/springframework/samples/petclinic/rest/advice/ExceptionControllerAdvice.java:170` · not re-checked

**Problem:** handleInvalidDto walks to the deepest cause and returns its message in schemaValidationErrors for every HttpMessageNotReadableException in the application. For malformed JSON the root cause is a Jackson parser message with token and location detail. For a missing body there is no cause, so the message is Spring's 'Required request body is missing: public ... method signature'. The same user-influenced text is logged at WARN unsanitised.

**Impact:** Clients receive controller signatures, class names and parser internals, and log lines can be forged with user-supplied text. Only InvalidDtoException messages (field names and lengths) are safe to echo.

**Recommendation:** Return the root-cause message only when the exception is an InvalidDtoException. Use a fixed message such as 'Malformed request body' for HttpMessageNotReadableException, and log without the raw user-controlled text or sanitise it.

### CR-003 · code · major — Null-only Bean Validation paths on DTOs untested

`src/main/java/org/springframework/samples/petclinic/rest/dto/PetDto.java:37` · not re-checked

**Problem:** PetDto.type and PetDto.birthDate, and VisitDto.petId (VisitDto.java:39), are enforced only by @NotNull through Bean Validation. Their compact constructors do not check them. No test sends PUT /api/pets/{id} without type or birthDate, or POST /api/visits without petId; every existing body supplies them. The Owner null-type tests hit POST /owners/{id}/pets, which binds PetFieldsDto, not PetDto.

**Impact:** If @NotNull is dropped or @Valid is lost from the generated mapping, PUT /api/pets/{id} would hit petMapper.toPetType(null) or persist a null birthDate (500 or bad data), and POST /api/visits would pass a null petId to the service. The current suite would still pass. The guard works today (VisitsApi.addVisit carries @Valid @RequestBody).

**Recommendation:** Add controller tests that PUT /api/pets/3 without type and without birthDate, and POST /api/visits without petId, each return 400. Alternatively add Validator assertions in PetDtoTests and VisitDtoTests.

## Coverage

Standards: commit-format na · code-quality na · security na

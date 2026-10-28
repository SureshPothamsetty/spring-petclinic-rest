package org.springframework.samples.petclinic.rest.validation;

/**
 * Thrown by the compact constructors of the hand-written DTO records when a value violates a constraint
 * declared in {@code openapi.yml}. Mapped to HTTP 400 by the REST exception advice.
 */
public class InvalidDtoException extends IllegalArgumentException {

    public InvalidDtoException(String message) {
        super(message);
    }
}

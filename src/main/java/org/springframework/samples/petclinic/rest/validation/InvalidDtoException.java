package org.springframework.samples.petclinic.rest.validation;

/**
 * Thrown by the compact constructors of the hand-written DTO records when a value violates a constraint
 * declared in {@code openapi.yml}. During request deserialization it surfaces wrapped in an
 * {@code HttpMessageNotReadableException}, which the REST exception advice maps to HTTP 400. Thrown anywhere else
 * (for example while mapping stored data to a response DTO) it is a server-side fault and results in HTTP 500.
 */
public class InvalidDtoException extends IllegalArgumentException {

    public InvalidDtoException(String message) {
        super(message);
    }
}

package com.edu.unimag.rodatours.services.exceptions;

/** La operación es incompatible con el estado actual de los recursos. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

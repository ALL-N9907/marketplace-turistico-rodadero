package com.edu.unimag.rodatours.services.exceptions;

/** Los datos o la combinación de parámetros recibidos no son válidos. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

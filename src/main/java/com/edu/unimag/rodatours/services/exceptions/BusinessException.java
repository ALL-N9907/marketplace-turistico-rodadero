package com.edu.unimag.rodatours.services.exceptions;

/** Una regla de negocio impide realizar la operación solicitada. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.edu.unimag.rodatours.exceptions;

import com.edu.unimag.rodatours.services.exceptions.BusinessException;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DomainExceptionTest {
    static Stream<Function<String, RuntimeException>> messageConstructors() {
        return Stream.of(BusinessException::new, ConflictException::new,
                ResourceNotFoundException::new, ValidationException::new);
    }

    static Stream<BiFunction<String, Throwable, RuntimeException>> causeConstructors() {
        return Stream.of(BusinessException::new, ConflictException::new,
                ResourceNotFoundException::new, ValidationException::new);
    }

    @ParameterizedTest
    @MethodSource("messageConstructors")
    void preservesPublicMessage(Function<String, RuntimeException> constructor) {
        RuntimeException exception = constructor.apply("La operación no está permitida.");
        assertEquals("La operación no está permitida.", exception.getMessage());
        assertNull(exception.getCause());
    }

    @ParameterizedTest
    @MethodSource("causeConstructors")
    void preservesOriginalCause(BiFunction<String, Throwable, RuntimeException> constructor) {
        Throwable cause = new IllegalStateException("Detalle interno");
        RuntimeException exception = constructor.apply("Mensaje público", cause);
        assertEquals("Mensaje público", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}

package com.edu.unimag.rodatours.exceptions;

import com.edu.unimag.rodatours.api.exceptions.GlobalExceptionHandler;
import com.edu.unimag.rodatours.services.exceptions.BusinessException;
import com.edu.unimag.rodatours.services.exceptions.ConflictException;
import com.edu.unimag.rodatours.services.exceptions.ResourceNotFoundException;
import com.edu.unimag.rodatours.services.exceptions.ValidationException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ErrorController.class,
        properties = "logging.level.com.edu.unimag.rodatours.api.exceptions.GlobalExceptionHandler=OFF")
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.ErrorController.class})
@WithMockUser
class GlobalExceptionHandlerTest {
    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @CsvSource({"not-found,404", "business,400", "validation,400", "conflict,409"})
    void mapsDomainExceptionsToProblemDetail(String type, int expectedStatus) throws Exception {
        mvc.perform(get("/test/errors/domain/{type}", type))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").value("Mensaje público"))
                .andExpect(jsonPath("$.instance").value("/test/errors/domain/" + type))
                .andExpect(content().string(not(containsString("detalle-interno"))));
    }

    @Test
    void reportsInvalidBodyFieldsWithoutExposingRejectedValues() throws Exception {
        mvc.perform(post("/test/errors/body").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"correo\":\"dato-confidencial-invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.errors.correo").value("El correo no es válido"))
                .andExpect(content().string(not(containsString("dato-confidencial-invalido"))));
    }

    @Test
    void reportsInvalidBodyAndPathInMethodsWithParameterValidation() throws Exception {
        mvc.perform(post("/test/errors/body/0").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"correo\":\"correo-invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.id").exists())
                .andExpect(jsonPath("$.errors.nombre").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.errors.correo").value("El correo no es válido"));
    }

    @Test
    void reportsNonpositivePathAndQueryParameters() throws Exception {
        mvc.perform(get("/test/errors/positive/0").param("cantidad", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.id").exists())
                .andExpect(jsonPath("$.errors.cantidad").exists());
    }

    @Test
    void reportsMissingRequiredParameter() throws Exception {
        mvc.perform(get("/test/errors/date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fecha").value("El parámetro es obligatorio."));
    }

    @ParameterizedTest
    @CsvSource({"/test/errors/date?fecha=invalid", "/test/errors/enum?estado=INVALID",
            "/test/errors/positive/not-a-number?cantidad=1"})
    void reportsTypeMismatchWithoutExposingFrameworkTypes(String url) throws Exception {
        mvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Un parámetro de la solicitud tiene un formato o valor no admitido."))
                .andExpect(jsonPath("$.errors").isMap())
                .andExpect(content().string(not(containsString("java.time"))))
                .andExpect(content().string(not(containsString("ErrorController"))));
    }

    @Test
    void reportsMalformedJsonWithoutParserInternals() throws Exception {
        mvc.perform(post("/test/errors/body").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"secreto-sin-cierre"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(content().string(not(containsString("secreto-sin-cierre"))))
                .andExpect(content().string(not(containsString("Jackson"))));
    }

    @Test
    void reportsMissingBody() throws Exception {
        mvc.perform(post("/test/errors/body").with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void reportsConstraintViolationsWithoutRejectedValues() throws Exception {
        mvc.perform(get("/test/errors/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.correo").value("El correo no es válido"))
                .andExpect(content().string(not(containsString("dato-confidencial-invalido"))));
    }

    @ParameterizedTest
    @CsvSource({"integrity", "pessimistic", "optimistic"})
    void reportsPersistenceConflictsWithoutSqlOrDatabaseDetails(String type) throws Exception {
        mvc.perform(get("/test/errors/persistence/{type}", type))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "La operación entra en conflicto con los datos actuales. Consulte el recurso e intente nuevamente."))
                .andExpect(content().string(not(containsString("SELECT"))))
                .andExpect(content().string(not(containsString("password"))));
    }

    @ParameterizedTest
    @CsvSource({"runtime", "checked", "illegal-argument"})
    void reportsUnexpectedErrorsWithoutTreatingProgrammingBugsAsBadRequests(String type) throws Exception {
        mvc.perform(get("/test/errors/unexpected/{type}", type))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ocurrió un error interno. Intente nuevamente más tarde."))
                .andExpect(jsonPath("$.instance").value("/test/errors/unexpected/" + type))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void reportsInvalidReturnValueAsInternalErrorWithoutValidationDetails() throws Exception {
        mvc.perform(get("/test/errors/invalid-return"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ocurrió un error interno. Intente nuevamente más tarde."))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void preservesMethodNotAllowedStatusAndAllowHeader() throws Exception {
        mvc.perform(put("/test/errors/date").with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void preservesUnsupportedContentTypeStatus() throws Exception {
        mvc.perform(post("/test/errors/body").with(csrf()).contentType(MediaType.TEXT_PLAIN).content("texto"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void preservesNotFoundForUnmappedPath() throws Exception {
        mvc.perform(get("/test/errors/unmapped"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void delegatesAuthorizationErrorsToSecurityFilters() throws Exception {
        mvc.perform(get("/test/errors/access-denied")).andExpect(status().isForbidden());
    }

    @Test
    void delegatesAuthenticationErrorsToSecurityFilters() throws Exception {
        mvc.perform(get("/test/errors/authentication").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void keepsUnauthenticatedRequestsBlocked() throws Exception {
        mvc.perform(get("/test/errors/date").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void keepsCsrfProtectionEnabled() throws Exception {
        mvc.perform(post("/test/errors/body").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    // Sólo se registra en este slice de pruebas; no agrega endpoints a la aplicación.
    @RestController
    @RequestMapping("/test/errors")
    static class ErrorController {
        private final Validator validator;

        ErrorController(Validator validator) {
            this.validator = validator;
        }

        @GetMapping("/domain/{type}")
        String domain(@PathVariable String type) {
            Throwable cause = new IllegalStateException("detalle-interno");
            throw switch (type) {
                case "not-found" -> new ResourceNotFoundException("Mensaje público", cause);
                case "business" -> new BusinessException("Mensaje público", cause);
                case "validation" -> new ValidationException("Mensaje público", cause);
                default -> new ConflictException("Mensaje público", cause);
            };
        }

        @PostMapping("/body")
        String body(@Valid @RequestBody Input input) {
            return "ok";
        }

        @PostMapping("/body/{id}")
        String bodyAndPath(@PathVariable @Positive Long id, @Valid @RequestBody Input input) {
            return "ok";
        }

        @GetMapping("/positive/{id}")
        String positive(@PathVariable @Positive Long id, @RequestParam @Positive int cantidad) {
            return "ok";
        }

        @GetMapping("/date")
        String date(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
            return "ok";
        }

        @GetMapping("/enum")
        String enumValue(@RequestParam State estado) {
            return "ok";
        }

        @GetMapping("/constraint")
        String constraint() {
            throw new ConstraintViolationException(validator.validate(new Input("Ana", "dato-confidencial-invalido")));
        }

        @GetMapping("/persistence/{type}")
        String persistence(@PathVariable String type) {
            String details = "SELECT password FROM turistas";
            throw switch (type) {
                case "integrity" -> new DataIntegrityViolationException(details);
                case "pessimistic" -> new CannotAcquireLockException(details);
                default -> new OptimisticLockingFailureException(details);
            };
        }

        @GetMapping("/unexpected/{type}")
        String unexpected(@PathVariable String type) throws Exception {
            throw switch (type) {
                case "runtime" -> new NullPointerException("password=secreto");
                case "illegal-argument" -> new IllegalArgumentException("password=secreto");
                default -> new Exception("password=secreto");
            };
        }

        @GetMapping("/invalid-return")
        @Positive
        int invalidReturn() {
            return -1;
        }

        @GetMapping("/access-denied")
        String accessDenied() {
            throw new AccessDeniedException("No autorizado");
        }

        @GetMapping("/authentication")
        String authentication() {
            throw new BadCredentialsException("Credenciales incorrectas");
        }
    }

    enum State { ACTIVE, INACTIVE }

    record Input(@NotBlank(message = "El nombre es obligatorio") String nombre,
                 @NotBlank @Email(message = "El correo no es válido") String correo) {
    }
}

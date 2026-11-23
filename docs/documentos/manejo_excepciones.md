# Manejo de excepciones

La rama `exception` completa el manejo de errores existente sin cambiar los
contratos de los servicios, los controladores ni la configuración de seguridad.

## Excepciones de aplicación

Ubicación: `src/main/java/com/edu/unimag/rodatours/services/exceptions/`.

| Excepción | HTTP | Uso |
|---|---|---|
| `ResourceNotFoundException` | 404 | Turista, agencia, tour, horario o reserva inexistente. |
| `BusinessException` | 400 | Regla de negocio incumplida, por ejemplo reservar en una fecha pasada. |
| `ValidationException` | 400 | Datos o combinaciones de filtros inválidos. |
| `ConflictException` | 409 | Cupos insuficientes, traslapes, duplicados o transición de estado incompatible. |

Cada excepción acepta un mensaje público y, opcionalmente, una causa original:

```java
throw new ConflictException("No fue posible confirmar la reserva.", cause);
```

El mensaje público debe ser comprensible para el cliente y no incluir SQL,
contraseñas ni detalles técnicos. La causa no se serializa en la respuesta.
Las excepciones no dependen de anotaciones HTTP: esa traducción pertenece a la API.

## Respuestas HTTP

`api/exceptions/GlobalExceptionHandler.java` centraliza los errores mediante
`@RestControllerAdvice` y `ProblemDetail` (RFC 9457).

Tipo de contenido: `application/problem+json`.

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "La solicitud contiene campos inválidos.",
  "instance": "/api/tourists",
  "errors": {
    "correoElectronico": "Debe proporcionar un formato de correo electrónico válido"
  }
}
```

- `title`: descripción estándar del estado HTTP.
- `status`: código de estado, consistente con el de la respuesta.
- `detail`: explicación pública del problema.
- `instance`: ruta de la solicitud.
- `errors`: mapa opcional de campos o parámetros a mensajes de validación.
  Si hay varios errores para un campo, se conserva el primero.
- Cuando se omite `type`, se entiende `about:blank`, conforme a RFC 9457.

### Errores de entrada

- JSON malformado, cuerpo obligatorio ausente o tipos JSON incompatibles: **400**
  con un mensaje genérico, sin información del parser.
- Campos inválidos con `@Valid`: **400**, con `errors` por campo; también admite
  errores de objeto completo.
- Parámetros de ruta o consulta inválidos: **400**, con `errors` por parámetro.
- Parámetros obligatorios ausentes: **400**, con el nombre del parámetro.
- Fechas, enums o identificadores que no pueden convertirse: **400**, sin mostrar
  valores rechazados ni clases internas.
- `ConstraintViolationException`: **400**, con rutas y mensajes de las restricciones.

No se agregan los valores rechazados al JSON de validación. Los mensajes de las
restricciones deben evitar interpolar información sensible.

### Persistencia y errores internos

- `DataIntegrityViolationException`: **409** con mensaje genérico, sin SQL ni
  nombres de tablas o restricciones.
- `ConcurrencyFailureException` y sus subclases (bloqueo pesimista/optimista):
  **409**, con el mismo mensaje seguro.
- Excepciones inesperadas: **500**, con el mensaje
  «Ocurrió un error interno. Intente nuevamente más tarde.».
- Errores de validación del valor de retorno de un controlador: **500**, no **400**.
- Otros errores internos gestionados por Spring MVC también se sanitizan como **5xx**.
- Los detalles técnicos de errores internos se registran sólo en el servidor;
  los logs deben tener controles de acceso y no exponerse a clientes.

No se convierten indiscriminadamente `IllegalArgumentException` o
`IllegalStateException` en errores de cliente: pueden indicar fallos de programación.
Los servicios deben usar las excepciones de aplicación para errores esperados.

### Spring MVC y seguridad

Se conservan los estados y cabeceras de Spring MVC, incluyendo **404**, **405**
(con `Allow`) y **415**.

`AuthenticationException` y `AccessDeniedException` se vuelven a propagar para
que Spring Security las gestione; no quedan atrapadas por el manejador genérico
de **500**. No se cambia el formato de los errores generados por los filtros de
seguridad ni se desactivan autenticación o CSRF.

## Pruebas

```powershell
# Pruebas específicas, sin Docker
.\mvnw.cmd "-Dtest=DomainExceptionTest,GlobalExceptionHandlerTest" test

# Suite completa (Docker activo para Testcontainers/PostgreSQL)
.\mvnw.cmd test
```

Las pruebas específicas verifican constructores, causas, estados HTTP, formato
Problem Detail, validación de entrada, persistencia/concurrencia, ocultación de
detalles internos, valores de retorno inválidos y compatibilidad con seguridad.
El controlador utilizado para provocar errores sólo existe dentro de las pruebas.

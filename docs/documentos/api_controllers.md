# API REST: capa controller

Implementación de los endpoints mínimos de `marketplace_turistico_rodadero.md`,
con la ampliación de guías. Las reseñas siguen fuera del alcance mínimo.

## Convenciones

- JSON con los nombres camelCase de los DTOs existentes.
- Creación: `201 Created`, cuerpo con el recurso y cabecera `Location` consultable.
- Consulta y actualización: `200 OK`. Las búsquedas sin resultados devuelven `[]`.
- Identificadores de ruta y consulta positivos; validación de cuerpos con `@Valid`.
- Fechas: `yyyy-MM-dd`; fechas con hora: `yyyy-MM-dd'T'HH:mm:ss`.
- Errores: `application/problem+json` (Problem Detail), con `status`, `title`,
  `detail` e `instance`. Los errores de validación añaden `errors` por campo/parámetro.
- `400`: entradas inválidas o reglas de negocio; `404`: recurso inexistente;
  `409`: conflictos de estado, cupos, unicidad o concurrencia.

## Endpoints

| Recurso | Operaciones |
|---|---|
| Turistas | `POST /api/tourists`, `GET /api/tourists`, `GET /api/tourists/{id}`, `PUT /api/tourists/{id}` |
| Agencias | `POST /api/agencies`, `GET /api/agencies`, `GET /api/agencies/{id}`, `PUT /api/agencies/{id}` |
| Tipos de experiencia | `POST /api/experience-types`, `GET /api/experience-types`, `GET /api/experience-types/{id}` |
| Tours | `POST /api/tours`, `GET /api/tours`, `GET /api/tours/{id}`, `PUT /api/tours/{id}` |
| Horarios | `POST /api/tours/{tourId}/schedules`, `GET /api/tours/{tourId}/schedules`, `GET /api/schedules/{id}` |
| Guías | `POST /api/guides`, `GET /api/guides`, `GET /api/guides/{id}`, `PUT /api/guides/{id}` |
| Asignar guía | `PUT /api/schedules/{id}/guide?guiaId=2` |
| Reservas | `POST /api/reservations`, `GET /api/reservations`, `GET /api/reservations/{id}` |
| Estados de reserva | `PUT /api/reservations/{id}/confirm`, `/cancel`, `/complete`, `/no-show` |
| Disponibilidad | `GET /api/availability/tours/{tourId}?fecha=2027-01-15&cantidadPersonas=2` |
| Ocupación | `GET /api/reports/agency-occupancy?agenciaId=1&desde=2026-01-01&hasta=2026-01-31` |
| Productividad | `GET /api/reports/agency-productivity?desde=2026-01-01&hasta=2026-01-31` |
| Inasistencias | `GET /api/reports/no-show-tourists?desde=2026-01-01&hasta=2026-01-31` |

### Filtros opcionales

- Turistas: `estado` (`ACTIVE` o `INACTIVE`).
- Agencias: `activeOnly=true`, o `query=Rodadero` para buscar agencias activas por
  nombre o dirección. `query` tiene prioridad sobre `activeOnly`.
- Tours: `agenciaId`, con `dificultad` y/o `destino` opcionales. Estos dos filtros
  requieren agencia; no se descartan filtros silenciosamente.
- Horarios de un tour: `fecha`.
- Guías: `agenciaId` y `estado` juntos, o ninguno.
- Reservas: una de las alternativas `turistaId` (con `estado` opcional),
  `horarioId`, o `desde` y `hasta` (fecha y hora de creación de la reserva).
  No se combinan alternativas; ambos extremos del rango son obligatorios.
- Disponibilidad: `fecha` obligatoria; `cantidadPersonas` positiva, por defecto `1`.
- Reportes: ambos extremos del rango obligatorios, con `desde <= hasta`.

### Reserva

Creación (`POST /api/reservations`):

```json
{"turistaId": 2, "horarioId": 3, "cantidadPersonas": 2, "observaciones": "Sin gluten"}
```

El cliente no define estado inicial, precio, fechas de inicio/fin ni código QR:
los calcula la capa de servicio. Las contraseñas nunca forman parte de las respuestas.

Cancelación: cuerpo obligatorio `{"motivo":"Cambio de planes"}`; no acepta motivo vacío.
Finalización: cuerpo opcional `{"observaciones":"El turista asistió"}`.
Confirmación e inasistencia: sin cuerpo.

## Arquitectura y seguridad

Los controladores sólo validan el contrato HTTP y delegan en servicios. Las reglas
de cupos, traslapes y transiciones permanecen en la capa service.
`AvailabilityServiceImpl` y `ReportServiceImpl` reutilizan los métodos existentes
de `TourScheduleService` y `ReservationService`, sin romper sus contratos ni duplicar consultas.

No se cambia la configuración de Spring Security: los endpoints conservan la
autenticación y la protección CSRF predeterminadas. Las pruebas usan `@WithMockUser`
y tokens CSRF para las escrituras. No se implementa autenticación de turistas ni
autorización por roles en esta capa; los perfiles del enunciado requieren un diseño
de seguridad específico antes de exponer el sistema en producción.

## Pruebas

Sólo controladores (sin Docker ni base de datos):

```powershell
.\mvnw.cmd "-Dtest=*ControllerTest" test
```

Regresión de controladores, servicios, mappers, validaciones y entidades:

```powershell
.\mvnw.cmd "-Dtest=*ControllerTest,*ServiceImplTest,*MapperTest,RequestValidationTest,*EntityTest,ReservationStatusTest" test
```

Suite completa, con Docker activo para Testcontainers/PostgreSQL:

```powershell
.\mvnw.cmd test
```

# Marketplace Turístico de El Rodadero

*Prototipo de API REST para la gestión de tours, cupos y reservas · Java 21 · Spring Boot 4 · PostgreSQL · Testcontainers · JUnit 5 · Mockito*

## 1. De qué trata el proyecto

Este proyecto es un **marketplace turístico**: una plataforma backend donde las agencias operadoras de El Rodadero (Santa Marta) publican sus tours y experiencias, como caminatas a la Sierra Nevada, visitas al Parque Tayrona, Minca o playas cristalinas, y donde los viajeros pueden consultar la disponibilidad real y reservar un cupo.

### El problema

Hoy la venta de tours se hace de forma manual, dispersa y desorganizada: redes sociales, sitios web aislados y volantes en la calle. Esto produce reservas duplicadas, precios poco transparentes, incertidumbre en los viajeros y muy poca trazabilidad sobre el estado de cada atención y la disponibilidad de cupos.

### La solución

Una API REST que centraliza la operación en un solo lugar:

- Un catálogo único de agencias, tipos de experiencia y tours, con precio y duración definidos.
- Una agenda de salidas por tour, con cupos controlados para evitar la sobreventa.
- Un ciclo de vida claro para cada reserva (programada, confirmada, cancelada, completada o no asistida), con trazabilidad de los cambios.
- Reportes operativos de ocupación, productividad de agencias e inasistencia de turistas.

### Objetivo académico

Construir el backend con **arquitectura por capas** (repository, service y controller), base de datos relacional y pruebas automatizadas:

- **Repository:** consultas derivadas y JPQL, probadas con Testcontainers.
- **Service:** reglas de negocio, validaciones de flujo y manejo de conflictos, probados con JUnit 5 y Mockito.
- **Controller:** API REST con validación de entrada y manejo de errores, probada con MockMvc.

## 2. Quiénes usan el sistema

- **Administrador:** registra turistas, agencias, tipos de experiencia y tours, configura los cupos y puede completar reservas o marcarlas como no asistidas.
- **Operador de agencia:** confirma y cancela las reservas de su agencia.
- **Guía:** dirige la salida y cierra la experiencia marcando la reserva como completada.
- **Usuario del sistema / viajero:** consulta disponibilidad y crea reservas.
- **Coordinador:** consulta los reportes para tomar decisiones.

## 3. Cómo funciona: el recorrido de una reserva

1. **Se arma el catálogo.** El administrador registra la agencia, el tipo de experiencia (por ejemplo, aventura o cultural) y el tour con su destino, precio y duración. También registra al turista.
2. **Se programa la agenda.** Para cada tour se definen las fechas y horas de salida con su cupo total. Cada salida lleva el control de cuántos cupos quedan y, opcionalmente, el guía que la dirige.
3. **Se consulta la disponibilidad.** Se pregunta por un tour en una fecha y el sistema responde solo con las salidas que tienen capacidad real.
4. **Se crea la reserva.** El sistema valida que el turista y el tour existan y estén activos, que la fecha no sea pasada, que haya cupos y que el turista no tenga otra reserva activa en el mismo horario. La hora de fin la calcula el servidor a partir de la duración del tour; el cliente nunca la envía. La reserva nace en estado `SCHEDULED`.
5. **Se confirma.** Una reserva `SCHEDULED` pasa a `CONFIRMED`. En este paso el sistema descuenta los cupos de forma concurrente y segura.
6. **Se cancela (opcional).** Una reserva `SCHEDULED` o `CONFIRMED` puede cancelarse indicando un motivo obligatorio. Los cupos se liberan de inmediato.
7. **Se cierra la experiencia.** El día del tour (o después), una reserva `CONFIRMED` pasa a `COMPLETED` si el turista asistió, o a `NO_SHOW` si no se presentó.

### Estados de la reserva

- `SCHEDULED`: reserva creada, pendiente de validación.
- `CONFIRMED`: validada por la agencia, con cupos descontados.
- `CANCELLED`: anulada con motivo, cupos liberados.
- `COMPLETED`: el turista realizó el tour.
- `NO_SHOW`: el turista no asistió.

Las transiciones válidas son: `SCHEDULED → CONFIRMED`, `SCHEDULED → CANCELLED`, `CONFIRMED → CANCELLED`, `CONFIRMED → COMPLETED` y `CONFIRMED → NO_SHOW`. Los estados `CANCELLED`, `COMPLETED` y `NO_SHOW` son finales: de ahí no se puede salir.

> **Punto a definir en el diseño:** el enunciado valida la disponibilidad al crear la reserva pero descuenta el stock al confirmar. Conviene decidir y documentar si las reservas `SCHEDULED` también cuentan contra el cupo al calcular la disponibilidad, para evitar que dos reservas pendientes ocupen el mismo último lugar.

## 4. Entidades y atributos

El modelo tiene siete tablas. Los nombres de atributos siguen el diagrama entidad-relación del proyecto; los que el enunciado exige pero el diagrama no traía se marcan como *(requerido por el enunciado)*.

### 4.1 `turistas` (Turistas)

Representa a la persona que viaja. Es el cliente final del sistema: sin turista registrado y activo no se puede crear ninguna reserva. Guarda su identidad, sus datos de contacto y las credenciales de acceso.

- `id`: identificador único de la tabla (clave primaria).
- `nombre_completo`: nombre y apellidos del turista, tal como aparecerá en la reserva.
- `correo_electronico`: correo de contacto y de inicio de sesión. Debe ser único para que no existan dos cuentas con el mismo correo.
- `contrasena`: credencial de acceso. Se almacena siempre como hash, nunca en texto plano.
- `telefono`: número de contacto para comunicarse con el viajero antes del tour (opcional).
- `estado`: indica si la cuenta está activa o inactiva (`TouristStatus`). Un turista inactivo no puede reservar, pero se conserva su historial.
- `fecha_creacion`: fecha y hora en que se registró en la plataforma.

### 4.2 `experience_types` (Tipos de experiencia)

Catálogo que clasifica las actividades turísticas (aventura, cultural, gastronómica, playa, naturaleza, etc.). Sirve para ordenar la oferta y para los reportes, por ejemplo contar cancelaciones e inasistencias por tipo de experiencia.

- `id`: identificador único (clave primaria).
- `nombre`: nombre de la categoría. Es único para no duplicar categorías.
- `descripcion`: explicación de qué actividades agrupa la categoría (opcional).

### 4.3 `agencies` (Agencias)

Empresa operadora que diseña, publica y ejecuta los tours. Es la dueña de la oferta: cada tour pertenece a una agencia, y las métricas de ocupación y productividad se calculan por agencia. Incluye su información corporativa y legal.

- `id`: identificador único (clave primaria).
- `nombre_comercial`: nombre con el que la agencia se presenta al público.
- `nit_o_registro`: NIT o registro legal que la identifica ante las autoridades. Es único.
- `descripcion`: presentación de la agencia y de su especialidad (opcional).
- `direccion`: ubicación física de la oficina o punto de atención. Se usa para buscar agencias por ubicación.
- `telefono_contacto`: teléfono de atención al cliente.
- `estado`: indica si la agencia está activa o inactiva (`AgencyStatus`). Una agencia inactiva no aparece en las búsquedas de agencias activas.

### 4.4 `tours` (Tours)

Es la experiencia turística que se vende: un recorrido o actividad concreta, con su precio, duración y dificultad. El tour describe *qué* se ofrece; el *cuándo* y el *cuántos cupos* viven en los horarios. La duración del tour es la base con la que el sistema calcula la hora de fin de cada reserva.

- `id`: identificador único (clave primaria).
- `agencia_id`: agencia que ofrece el tour (clave foránea a `agencies`).
- `tipo_experiencia_id`: categoría del tour (clave foránea a `experience_types`).
- `titulo`: nombre comercial del tour, por ejemplo «Caminata a Minca».
- `descripcion`: detalle de la actividad, el recorrido y lo que vivirá el turista.
- `destino`: lugar donde se realiza (Tayrona, Minca, Sierra Nevada, etc.). Permite consultar disponibilidad por destino.
- `precio`: valor por persona. Debe ser mayor que cero.
- `duracion_horas`: duración de la experiencia. Con ella el servicio calcula la hora de fin de la reserva.
- `nivel_dificultad`: exigencia física de la actividad (por ejemplo bajo, medio o alto), útil para que el viajero elija según su condición.
- `servicios_incluidos`: lo que cubre el precio, como transporte, guía, alimentación o seguro (opcional).
- `estado`: indica si el tour está activo o inactivo. Un tour inactivo no se puede reservar.

La capacidad concreta de cada salida no se guarda aquí sino en `tour_schedules`, porque un mismo tour puede tener salidas con cupos distintos según el día.

### 4.5 `tour_schedules` (Horarios y cupos)

Es la agenda del tour: cada registro es una salida en una fecha y hora específicas, con su capacidad. Es la tabla que hace posible el control de cupos y evita la sobreventa, porque aquí se lleva la cuenta de cuántos lugares quedan.

- `id`: identificador único (clave primaria).
- `tour_id`: tour al que pertenece la salida (clave foránea a `tours`).
- `guia_id`: guía asignado a la salida (clave foránea a `guides`). Es opcional: puede quedar vacío hasta que se asigne un guía de la misma agencia del tour.
- `fecha_tour`: fecha en que sale el tour.
- `hora_inicio`: hora de salida. No pueden existir dos salidas del mismo tour con la misma fecha y hora.
- `cupos_totales`: capacidad máxima de la salida.
- `cupos_disponibles`: cupos que todavía se pueden reservar. Nunca puede ser negativo ni superar `cupos_totales`. Disminuye al confirmar una reserva y aumenta al cancelarla.

### 4.6 `reservations` (Reservas)

Es la entidad central del sistema: une a un turista con una salida de un tour. Concentra el estado del proceso, el rango de tiempo que ocupa la experiencia y la trazabilidad de los cambios.

- `id`: identificador único (clave primaria).
- `turista_id`: turista que reserva (clave foránea a `turistas`).
- `horario_id`: salida reservada (clave foránea a `tour_schedules`). A través de ella se llega al tour y a la agencia.
- `fecha_reserva`: fecha y hora en que se creó la reserva.
- `fecha_inicio` y `fecha_fin` *(requerido por el enunciado)*: rango de tiempo que ocupa la experiencia. La fecha de fin la calcula el servicio sumando la duración del tour a la de inicio; no la envía el cliente. Con este rango se detecta si el turista tiene otra reserva activa que se cruce.
- `cantidad_personas`: número de personas incluidas en la reserva. Es lo que se descuenta de los cupos.
- `precio_total`: valor total, calculado como precio del tour por cantidad de personas al momento de reservar. Se guarda para que un cambio posterior de precio no altere reservas ya hechas.
- `estado`: etapa actual (`ReservationStatus`): `SCHEDULED`, `CONFIRMED`, `CANCELLED`, `COMPLETED` o `NO_SHOW`. Toda reserva nueva inicia en `SCHEDULED`.
- `motivo_cancelacion`: razón de la cancelación. Es obligatorio cuando el estado es `CANCELLED` y vacío en los demás casos.
- `observaciones`: notas del turista (alergias, necesidades especiales) o notas administrativas al finalizar la reserva (opcional).
- `codigo_qr`: código único para validar la reserva el día del tour.
- `fecha_modificacion` *(requerido por el enunciado)*: fecha y hora del último cambio de estado, que junto con `fecha_reserva` da la trazabilidad básica.

> **Nota de diseño:** el enunciado dice que un tour tiene muchas reservas. En este modelo esa relación se obtiene a través del horario (`reservations → tour_schedules → tours`). Si el equipo prefiere consultarla directamente, puede agregar `tour_id` a `reservations`, a costa de una pequeña redundancia.

> **Opcional:** el diagrama entidad-relación incluye también una entidad `reseñas` (calificación de 1 a 5, comentario y fecha) ligada 1:1 a una reserva completada. El enunciado no la exige dentro del alcance mínimo, así que puede dejarse como ampliación.

### 4.7 `guides` (Guías)

Es el profesional que acompaña y dirige el tour el día de la salida. Con esta entidad el sistema sabe quién es el guía y qué salidas tiene a cargo, en lugar de limitarse a registrar que alguien cierra la experiencia (HU-10). Es una ampliación del modelo mínimo del enunciado.

- `id`: identificador único (clave primaria).
- `agencia_id`: agencia a la que pertenece el guía (clave foránea a `agencies`).
- `nombre_completo`: nombre y apellidos del guía.
- `documento_identidad`: cédula o documento del guía. Es único.
- `registro_guia`: número de su registro o tarjeta profesional de guía de turismo. Es único y acredita que puede ejercer.
- `correo_electronico`: correo de contacto. Es único.
- `telefono`: número para coordinar la salida.
- `idiomas`: idiomas en los que puede guiar, por ejemplo español e inglés (opcional).
- `estado`: indica si el guía está activo o inactivo. Un guía inactivo no se puede asignar a salidas nuevas.
- `fecha_creacion`: fecha y hora de registro.

Sus relaciones son dos: una agencia tiene muchos guías (`agencies` 1:N `guides`) y un guía dirige muchas salidas (`guides` 1:N `tour_schedules`). Reglas recomendadas: el guía asignado a una salida debe ser de la misma agencia que ofrece el tour, y no puede tener dos salidas que se crucen en fecha y hora.

## 5. Relaciones entre entidades

- **Un tipo de experiencia tiene muchos tours** (`experience_types` 1:N `tours`). Cada tour pertenece a un solo tipo.
- **Una agencia tiene muchos tours** (`agencies` 1:N `tours`). Cada tour lo ofrece una sola agencia.
- **Un tour tiene muchos horarios** (`tours` 1:N `tour_schedules`). Cada salida pertenece a un solo tour.
- **Un turista tiene muchas reservas** (`turistas` 1:N `reservations`). Cada reserva es de un solo turista.
- **Un horario recibe muchas reservas** (`tour_schedules` 1:N `reservations`) hasta agotar sus cupos. Cada reserva apunta a una sola salida.
- **Una agencia tiene muchos guías** (`agencies` 1:N `guides`). Cada guía trabaja para una sola agencia.
- **Un guía dirige muchas salidas** (`guides` 1:N `tour_schedules`). Cada salida tiene como máximo un guía, que puede asignarse más adelante.

La cadena completa es `agencies → tours → tour_schedules → reservations ← tourists`, con `guides` colgando de la agencia y asignado a cada salida.

## 6. Reglas de negocio principales

### Al crear una reserva

- El turista y el tour deben existir y estar activos.
- La fecha no puede ser pasada.
- Debe haber cupos disponibles en la salida solicitada.
- El turista no puede tener otra reserva activa que se cruce en fecha y hora.
- La hora de fin la calcula el servicio con la duración del tour.
- El estado inicial es siempre `SCHEDULED`.

### Al confirmar

- Solo se confirma una reserva `SCHEDULED`.
- Se descuentan los cupos de forma concurrente y segura (bloqueo de fila o equivalente).
- Se actualiza la fecha de modificación.

### Al cancelar

- Solo se cancelan reservas `SCHEDULED` o `CONFIRMED`.
- El motivo es obligatorio.
- Los cupos se liberan de inmediato.

### Al completar o marcar como no asistida

- Solo una reserva `CONFIRMED` puede pasar a `COMPLETED` o a `NO_SHOW`.
- No se puede completar si la fecha del tour es posterior a hoy, ni marcar `NO_SHOW` antes de la fecha programada.
- Al completar se pueden registrar observaciones administrativas.

### Al asignar un guía a una salida

- El guía debe estar activo y pertenecer a la misma agencia que ofrece el tour.
- El guía no puede tener otra salida que se cruce en fecha y hora.

## 7. Consultas y reportes

### Búsquedas básicas (consultas derivadas)

- Agencias activas por nombre o ubicación.
- Reservas de un turista por estado.
- Tours de una agencia por nivel de dificultad o destino.
- Reservas por rango de fechas.

### Validaciones y reportes (JPQL / agregación)

- Cupos disponibles de un tour en una fecha concreta, y los bloques con capacidad real.
- Traslape de reservas activas de un turista.
- Ocupación diaria o por rango de las agencias.
- Cantidad de reservas canceladas y no asistidas por tipo de experiencia.
- Ranking de agencias por tours completados.
- Turistas con más inasistencias (`NO_SHOW`) en un período.

### Atributos de cada reporte

Los reportes no son tablas: se calculan en el momento de la consulta con las tablas existentes y se devuelven en un DTO. Estos son los datos que trae cada uno.

- **Ocupación de agencias** (`AgencyOccupancyResponse`, `GET /api/reports/agency-occupancy`):
    - `agencia_id` y `nombre_comercial`: agencia analizada.
    - `fecha_desde` y `fecha_hasta`: rango consultado.
    - `cupos_totales`: suma de los cupos de todas sus salidas en el rango.
    - `cupos_ocupados`: cupos tomados por reservas confirmadas o completadas.
    - `porcentaje_ocupacion`: cupos ocupados sobre cupos totales.
- **Productividad de agencias** (`AgencyProductivityResponse`, `GET /api/reports/agency-productivity`):
    - `agencia_id` y `nombre_comercial`: agencia analizada.
    - `tours_completados`: cantidad de reservas que llegaron a `COMPLETED`.
    - `posicion`: lugar de la agencia en el ranking.
- **Turistas con inasistencias** (`NoShowTouristResponse`, `GET /api/reports/no-show-tourists`):
    - `turista_id` y `nombre_completo`: turista analizado.
    - `cantidad_no_show`: veces que fue marcado como `NO_SHOW` en el período.
    - `fecha_desde` y `fecha_hasta`: período consultado.

Para estos reportes se leen `agencies`, `tours`, `tour_schedules`, `reservations` y `turistas`, pero ninguno tiene relación propia con una tabla.

## 8. Alcance técnico

### Endpoints mínimos

- **Turistas:** `POST /api/tourists`, `GET /api/tourists/{id}`, `GET /api/tourists`, `PUT /api/tourists/{id}`.
- **Agencias:** `POST /api/agencies`, `GET /api/agencies/{id}`, `GET /api/agencies`, `PUT /api/agencies/{id}`.
- **Tipos de experiencia:** `POST /api/experience-types`, `GET /api/experience-types`.
- **Tours:** `POST /api/tours`, `GET /api/tours`, `PUT /api/tours/{id}`.
- **Horarios:** `POST /api/tours/{tourId}/schedules`, `GET /api/tours/{tourId}/schedules`.
- **Reservas:** `POST /api/reservations`, `GET /api/reservations/{id}`, `GET /api/reservations`, y los cambios de estado `PUT /api/reservations/{id}/confirm`, `/cancel`, `/complete` y `/no-show`.
- **Disponibilidad y reportes:** `GET /api/availability/tours/{tourId}`, `GET /api/reports/agency-occupancy`, `GET /api/reports/agency-productivity`, `GET /api/reports/no-show-tourists`.

### Clases a implementar

- **Entidades:** `Tourist`, `ExperienceType`, `Agency`, `Tour`, `TourSchedule`, `Reservation`.
- **Enums:** `ReservationStatus`, `AgencyStatus`, `TouristStatus`.
- **Repositories, services, implementaciones, controllers y mappers:** uno por cada entidad, más `AvailabilityService` y `ReportService` con sus controllers.
- **DTOs:** solicitudes de creación y actualización por entidad, `CancelReservationRequest`, y respuestas por entidad más `AvailabilitySlotResponse`, `AgencyOccupancyResponse`, `AgencyProductivityResponse` y `NoShowTouristResponse`.
- **Excepciones:** `ResourceNotFoundException`, `BusinessException`, `ConflictException`, `ValidationException` y `GlobalExceptionHandler`.
- **Ampliación (guías):** si se implementa la entidad `guides`, se suman `Guide`, `GuideRepository`, `GuideService`, `GuideServiceImpl`, `GuideController`, `GuideMapper`, `CreateGuideRequest`, `UpdateGuideRequest`, `GuideResponse` y un `GuideStatus`.

### Pruebas obligatorias

- **Repositorios (Testcontainers):** `TouristRepositoryIntegrationTest`, `AgencyRepositoryIntegrationTest`, `TourScheduleRepositoryIntegrationTest`, `ReservationRepositoryIntegrationTest` y `TourRepositoryIntegrationTest`. Cubren búsqueda de agencias por nombre, cupos y traslapes, reservas por rango de fechas, ocupación e inasistencias.
- **Servicios (JUnit 5 y Mockito):** `ReservationServiceImplTest`, `AvailabilityServiceImplTest` y `TourScheduleServiceImplTest`. Cubren rechazo de reservas en el pasado, traslapes y exceso de cupos, cálculo de duración, y cancelar, completar y marcar `NO_SHOW`.
- **Controladores (MockMvc):** `ReservationControllerTest`, `AgencyControllerTest`, `TouristControllerTest` y `AvailabilityControllerTest`.

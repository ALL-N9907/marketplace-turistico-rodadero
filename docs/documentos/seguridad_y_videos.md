# Primera etapa: cuentas, permisos y videos de tours

## Ejecutar localmente

Se requiere Java 21 y PostgreSQL. Configure las variables de entorno del proceso:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/rodatours"
$env:DB_USERNAME = "rodatours"
$env:DB_PASSWORD = "<contraseña de la base>"
$env:ADMIN_EMAIL = "<correo del administrador>"
$env:ADMIN_PASSWORD = "<contraseña de al menos 12 caracteres y máximo 72 bytes UTF-8>"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

`local` utiliza `ddl-auto=update` únicamente para desarrollo. El perfil normal usa
`validate`: no crea ni modifica tablas. Antes de un despliegue con datos existentes
se necesitan migraciones versionadas, incluidas las columnas de versión y las
nuevas tablas `staff_accounts`, `tour_videos` y `notifications`, además de coordenadas,
detalles de tours y fecha de check-in. No se borra ni migra una base existente automáticamente.

El bootstrap sólo crea una cuenta si ese correo no existe: **no restablece** ni
eleva el rol de cuentas existentes. Elimine las variables ADMIN_* del entorno
después de crear el primer administrador. No hay contraseñas predeterminadas.

## Autenticación

Las solicitudes autenticadas usan HTTP Basic (correo y contraseña). Requiere
**HTTPS** fuera de desarrollo. No se implementan JWT ni recuperación de contraseñas
en esta etapa.

- `GET /api/auth/csrf`: obtiene `headerName`, `parameterName` y `token`.
  Conserve la cookie de sesión y envíe el token en la cabecera indicada en cada escritura.
- `GET /api/auth/me`: comprueba las credenciales y devuelve rol e identificadores,
  nunca la contraseña ni su hash.
- `POST /api/auth/register`: registro público de turistas; cuerpo:

```json
{"name":"Ana Pérez","email":"ana@example.com","password":"una-contraseña-larga","phone":"3001234567"}
```

- `POST /api/auth/accounts`: sólo ADMIN; crea personal con rol explícito:

```json
{"email":"operador@example.com","password":"otra-contraseña-larga","role":"AGENCY_OPERATOR","agencyId":1}
```

El rol GUIDE requiere también `guideId` de un guía activo de la misma agencia.
ADMIN y COORDINATOR no admiten agencia/guía. TOURIST utiliza el registro de turistas.
Una agencia inactiva impide autenticar a sus operadores; un guía inactivo también
impide autenticar su cuenta.

### Permisos

| Operación | Acceso |
|---|---|
| Consultar catálogo/disponibilidad/video publicado/reseña por ID | Público |
| Crear agencias, tipos y turistas administrativos | ADMIN |
| Modificar agencia | ADMIN o su operador |
| Gestionar tour/horario/video/guía | ADMIN o el operador de la agencia propietaria |
| Crear reserva | ADMIN o el turista del cuerpo de la solicitud |
| Consultar reserva | ADMIN, turista dueño, operador de la agencia o guía asignado |
| Confirmar | ADMIN u operador de la agencia |
| Cancelar | ADMIN, turista dueño u operador de la agencia |
| Completar | ADMIN o guía asignado a la salida |
| Marcar NO_SHOW | ADMIN |
| Listar reservas | ADMIN; turistas con su `turistaId`; operadores con un `horarioId` propio |
| Ocupación de una agencia | ADMIN, COORDINATOR u operador de esa agencia |
| Rankings y reporte de inasistencias | ADMIN o COORDINATOR |
| Crear/modificar/eliminar reseñas | Turista dueño; al crear, reserva completada |

Los endpoints generales de turistas y sus actualizaciones siguen limitados a ADMIN
para no permitir cambios de estado de cuenta desde un perfil público.

## Cloudinary: carga directa firmada

Configure estas variables sólo en el servidor:

```text
CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET
CLOUDINARY_VIDEO_PRESET
```

En Cloudinary cree un **upload preset firmado** para videos, restringido a MP4,
WebM y MOV y con límite de tamaño de 50 MiB (o menor según su plan). No use un
preset unsigned para este recorrido. Conserve las restricciones en el preset
para rechazar archivos grandes antes de consumir almacenamiento.

El backend también verifica tamaño, duración (máximo 120 segundos), formato y
dimensiones consultando la API de Cloudinary. **No acepta la URL ni las dimensiones
enviadas por el navegador como prueba de una carga válida.**

### Recorrido

1. El operador llama `POST /api/tours/{tourId}/video/upload-authorization`.
   Recibe URL del Upload API, `apiKey`, `parameters`, `signature`, vencimiento y límites.
2. El cliente carga el archivo directamente a la `uploadUrl` mediante `multipart/form-data`:
   - `file`: archivo seleccionado.
   - `api_key`: el valor de `apiKey`.
   - `signature`: firma recibida.
   - Todos los pares de `parameters` sin cambiarlos, incluidos `public_id`,
     `timestamp`, `overwrite=false` y `upload_preset`.
3. Tras una respuesta exitosa de Cloudinary, llama `POST /api/tours/{tourId}/video/complete`,
   **sin cuerpo**. El servidor consulta los metadatos con sus credenciales y publica la URL sólo si son válidos.
4. `GET /api/tours/{tourId}/video` devuelve URL HTTPS y metadatos del video verificado.
5. Para reemplazar: `DELETE /api/tours/{tourId}/video`, después repetir el recorrido.
   La eliminación solicita invalidación de caché; la propagación en el CDN no es instantánea.

Hay **un video/carga por tour**. La autorización vence a la hora. Las cargas
pendientes, expiradas o inválidas no se publican desde la API, pero requieren
eliminación explícita para liberar el registro/activo e iniciar una nueva.
Si Cloudinary falla al eliminar, se conserva el registro para permitir reintentar.
Las llamadas externas tienen tiempos límite de conexión y lectura.

El activo subido usa el tipo público `upload` de Cloudinary; la verificación controla
su publicación en nuestra API, **no proporciona moderación ni privacidad del activo
externo**. No subir información personal. Moderación, carga privada y limpieza
automática de sesiones abandonadas quedan para endurecimiento posterior.

No se envía `api_secret` al cliente ni se guarda en el repositorio. El `api_key`
y la firma de carga sí forman parte de la autorización temporal. Sin credenciales
configuradas, las operaciones del proveedor responden **503**, no simulan éxito.
No se han creado cuentas, presets ni cargas reales en Cloudinary desde el agente.

## Endpoints completados

- `DELETE /api/tours/{id}` (sin reservas ni video asociado; elimine el video primero).
- `PUT /api/schedules/{id}`, `DELETE /api/schedules/{id}`.
- `POST /api/reviews`, `GET /api/reviews` (propias), `GET /api/reviews/{id}`,
  `PUT /api/reviews/{id}`, `DELETE /api/reviews/{id}`.

## Catálogo ampliado y comparación

- `GET /api/catalog/tours`: catálogo público activo, paginado con `page` (base 0) y
  `size` (1–100). Filtros opcionales: `destination`, `agencyId`, `experienceTypeId`,
  `difficulty`, `minPrice`, `maxPrice`, `minHours`, `maxHours`, `supportsCommunities`
  y `environmentalPractices`. Orden por precio ascendente e ID.
- `GET /api/catalog/tours/compare?ids=1,2`: compara de 2 a 6 IDs distintos y activos.
- `GET/PUT /api/tours/{id}/details`: itinerario, condiciones y sostenibilidad.
  El PUT exige operador propietario o ADMIN. Ejemplo de cuerpo completo:

```json
{"itinerary":"Visita a Minca y caminata","conditions":"Llevar agua",
 "supportsCommunities":true,"environmentalPractices":false}
```

- `GET/PUT /api/agencies/{id}/location`: coordenadas y URL opcional de foto.
  Latitud y longitud deben enviarse juntas y estar dentro de sus rangos geográficos.
  `photoUrl` admite únicamente imágenes HTTPS ya alojadas en Cloudinary; **este endpoint
  no carga fotografías**. El PUT exige operador propietario o ADMIN.
- `GET /api/reports/experience-type-cancellations?desde=2026-01-01&hasta=2026-12-31`:
  conteos de CANCELLED y NO_SHOW por tipo, restringido a ADMIN/COORDINATOR.

Las prácticas sostenibles son declaraciones del operador, no certificaciones verificadas.
No hay todavía búsqueda geográfica por distancia ni combinación del comparador con
la agenda de disponibilidad. Esta última conserva su endpoint específico.

## Voucher y check-in

- `GET /api/reservations/{id}/voucher`: comprobante JSON con estado, salida, precio y payload QR.
- `GET /api/reservations/{id}/voucher/qr`: QR PNG descargable (320×320).
- Ambos son privados: ADMIN, dueño, operador de la agencia o guía asignado.
- `POST /api/reservations/{id}/check-in`: sólo ADMIN, operador propietario o guía asignado.

```json
{"qrPayload":"rodatours:reservation:<codigoQr-de-la-reserva>"}
```

Sólo una reserva CONFIRMED y con salida **hoy** puede canjearse. Un segundo canje,
un estado inválido o una fecha distinta produce 409; un código incorrecto, 400.
El canje usa bloqueo de fila y registra `fechaCheckIn`, sin completar automáticamente
la reserva ni cambiar los cupos. El QR no contiene nombre, correo ni datos de pago.
Se usa la fecha local del servidor, como en el resto del flujo: configure una zona
horaria operativa coherente antes de desplegar. Las respuestas del voucher llevan `no-store`.

## Notificaciones internas y SMTP

- `GET /api/notifications?page=0&size=20`: bandeja paginada del usuario autenticado.
- `PUT /api/notifications/{id}/read`: marca como leída sólo una notificación propia;
  un ID ajeno devuelve 404.
- Crear, confirmar, cancelar, completar y marcar inasistencia de reservas generan
  avisos al turista y a los operadores activos de la agencia.
- Los avisos se persisten antes del commit y dentro de la transacción de la reserva.
  Una reserva revertida no produce avisos. No se incluye información sensible en el correo.

El correo queda en cola con `MAIL_ENABLED=false` (predeterminado). Para habilitar
entrega real configure:

```text
MAIL_ENABLED=true
SMTP_HOST=<servidor SMTP>
SMTP_PORT=587
SMTP_USERNAME=<usuario>
SMTP_PASSWORD=<secreto>
SMTP_FROM=<remitente autorizado>
SMTP_AUTH=true
SMTP_STARTTLS=true
```

El despachador procesa hasta 20 avisos por lote, con tiempos límite SMTP de 5 segundos
y hasta 5 intentos con espera creciente. No revertirá una reserva por un fallo SMTP.
La notificación interna permanece disponible aunque falle el correo. Tras 5 fallos
el aviso se conserva sin nuevos intentos automáticos; queda pendiente una interfaz
administrativa para reenvío y monitoreo.

SMTP no garantiza entrega exactamente una vez: un corte después del envío y antes
del commit podría duplicar un correo. No se han enviado correos reales durante el desarrollo.

## Pruebas

```powershell
# Adaptador Cloudinary sin llamadas externas y reglas de permisos/video
.\mvnw.cmd "-Dtest=CloudinaryVideoProviderTest,TourVideoServiceTest,AccessPolicyTest" test

# Recorrido HTTP con autenticación real y PostgreSQL (Docker activo)
.\mvnw.cmd "-Dtest=SecurityIntegrationTest,ReservationConcurrencyIntegrationTest" test

# Todas las pruebas y empaquetado
.\mvnw.cmd verify
```

Las pruebas de Cloudinary verifican firmas y respuestas HTTP con un servidor
simulado; el recorrido de seguridad usa BCrypt y PostgreSQL reales, con proveedor
de video simulado. Debe validarse una carga real en su cuenta antes del despliegue.

## Próximas etapas confirmadas

Mercado Pago **en pruebas** sigue pendiente; no se configuraron cobros. También
faltan las interfaces web, migraciones de producción y las ampliaciones indicadas
en el seguimiento de `funcionalidades.md`.

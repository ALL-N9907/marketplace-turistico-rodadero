# Ampliación de operación: QR, reseñas e historial

> Documento de la primera ampliación. Para sesiones, pagos, moderación y resultados
> posteriores con PostgreSQL, consultar [`guia_operativa.md`](guia_operativa.md).

## 1. Canjear un QR sin introducir el ID de reserva

```http
POST /api/reservations/check-in
Content-Type: application/json

{"qrPayload":"rodatours:reservation:<codigo-qr>"}
```

- Sólo ADMIN, operador de la agencia propietaria o guía asignado. Requiere
  autenticación y CSRF. No basta con conocer el QR.
- Busca bajo bloqueo pesimista y reutiliza las reglas del canje por ID: reserva
  CONFIRMED, salida hoy según el servidor y canje único. No completa ni descuenta cupos.
- La lectura de cámara no envía el canje: el usuario pulsa validar explícitamente.
- QR mal formado: 400. QR desconocido o fuera del alcance de la cuenta: mismo 404,
  sin revelar ID o datos. Uso previo/estado/fecha no válidos: 409.
- Respuesta: el mismo Voucher privado del endpoint anterior, con `Cache-Control: no-store`.
- El endpoint antiguo `POST /api/reservations/{id}/check-in` se conserva.
- No se cambia el payload ni se invalidan QRs ya emitidos; no se ponen tokens en URLs
  ni se crea un endpoint público para resolverlos.

## 2. Reseñas públicas y promedio

```http
GET /api/tours/1/reviews?page=0&size=5
```

```json
{
  "content": [{"id": 8, "rating": 5, "comment": "Excelente", "createdAt": "2026-11-23T12:00:00"}],
  "totalElements": 1,
  "page": 0,
  "size": 5,
  "totalPages": 1,
  "averageRating": 5.0
}
```

- Público, sólo para un tour y agencia activos. No disponible: 404, no lista ficticia.
- Página base cero y tamaño 1–100. Orden fecha de creación descendente e ID descendente.
- Lista y promedio consideran sólo reseñas de reservas COMPLETED del tour solicitado.
  El promedio es de todo el conjunto, no de la página; sin reseñas es `null`, no cero.
- No devuelve ID de reserva/turista, nombre, correo, teléfono ni QR. El comentario es
  contenido público escrito por su autor: no hay detección automática de información
  personal ni moderación en esta entrega. El frontend renderiza texto, nunca HTML.
- Alta, edición y eliminación siguen limitadas al titular autenticado. La interfaz
  invalida el listado público tras modificar una reseña.

## 3. Historial de estados

```http
GET /api/reservations/1/history?page=0&size=20
```

Respuesta paginada: `content`, `totalElements`, `page`, `size`, `totalPages`.
Cada evento incluye `id`, `previousStatus`, `status`, `occurredAt`, `actorRole`, `reason`.
El estado previo es `null` para la creación. Orden cronológico y por ID para desempates.

Los permisos son los mismos que para leer la reserva: ADMIN, titular, operador
propietario o guía asignado. Es privado y usa `Cache-Control: no-store`.

Los cambios emiten snapshots inmutables que un listener síncrono persiste dentro
de la misma transacción (propagación MANDATORY). El fallo de cualquiera de las
operaciones revierte reserva, cupos e historial; un intento rechazado no crea eventos.
Se guarda internamente el correo y rol del actor para auditoría; sólo se devuelve
el rol. Procesos internos sin principal se identifican como SYSTEM.

No hay API de edición/eliminación del historial. La inmutabilidad es de aplicación
(columnas no actualizables por JPA), no una garantía contra un administrador SQL.
No se registran por ahora notas independientes ni check-in, porque no cambian el estado.
No se inventan eventos para reservas anteriores a esta entrega.

## Actualización de la base

**No borrar la base de datos.** En desarrollo, arrancar con perfil local permite
crear la tabla nueva. Para una base existente con `ddl-auto=validate`, hacer copia
de seguridad y ejecutar una sola vez [`20261123_reservation_history.sql`](../sql/20261123_reservation_history.sql)
con el propietario del esquema antes de arrancar el backend nuevo.

El SQL es aditivo, no requiere credenciales nuevas ni modifica los QRs. No ejecutarlo
si local ya creó la tabla. La adopción de Flyway/Liquibase y la línea base completa
siguen pendientes; este script no resuelve migraciones antiguas faltantes.

## Verificación

Sin PostgreSQL/Docker:

```powershell
.\mvnw.cmd "-Dtest=QrCheckInServiceTest,PublicTourReviewServiceTest,ReservationHistoryTest,*ControllerTest,ReservationServiceImplTest,ReservationVoucherServiceTest,ReviewServiceImplTest,AccessPolicyTest" test
```

Con Docker activo (PostgreSQL de prueba, nunca la base del usuario):

```powershell
.\mvnw.cmd "-Dtest=SecurityIntegrationTest,ReviewRepositoryIntegrationTest,ReservationHistoryTransactionIntegrationTest" test
```

Frontend, dentro de `frontend/`:

```powershell
npm test
npm run build
$env:PLAYWRIGHT_CHANNEL = "chrome"
npm run test:e2e
```

Las pruebas de navegador emplean dobles HTTP. Las de PostgreSQL verifican consultas,
permisos con cuentas reales y rollback del historial; requieren Docker iniciado.
No se ha validado la base local del usuario ni solucionado su error de arranque
sin el registro que contiene la causa. No se han configurado proveedores externos.

Resultado de esta entrega: compilaciones Java y frontend correctas; **101 pruebas
backend sin contenedor, 31 pruebas frontend y 16 pruebas de navegador** correctas.
Las pruebas PostgreSQL quedaron compiladas, pero no ejecutadas porque el motor
Docker estaba detenido. No se ha borrado ni modificado la base del usuario.

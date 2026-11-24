# Funcionalidades del Prototipo: plataforma de agencias de turismo unificada para sector específico del rodadero

> Configuración y límites actuales: [`guia_operativa.md`](guia_operativa.md).

## Funcionalidades para el Usuario Final (Turista / Viajero):

1. **Vitrina y mapa geolocalizado:** Permite explorar de forma visual y rápida las agencias formales y los tours disponibles que operan desde el rodadero.
2. **Comparador inteligente de ofertas:** Filtros avanzados para comparar en tiempo real precios, duración de los recorridos, niveles de dificultad física y prácticas sostenibles (apoyo a comunidades locales o cuidado ambiental).
3. **Pasarela de pagos seguros:** Permite realizar transacciones en línea protegidas, eliminando el riesgo de fraudes, estafas o la dependencia de efectivo y transferencias manuales dudosas.
4. **Voucher digital con código QR:** Comprobante de reserva instantáneo que se puede redimir directamente con la agencia o el guía autorizado, agilizando el acceso a la experiencia.
5. **Sistema de calificaciones y reseñas reales:** Espacio para evaluar la experiencia vivida tras finalizar el tour, ayudando a otros viajeros a tomar decisiones informadas y premiando la calidad de los mejores operadores.

## Funcionalidades para las agencias (prestadores de servicios turísticos):

1. **Panel de gestión y autenticación de agencias:** módulo de acceso mediante credenciales que permite a cada agencia ingresar a su espacio privado y administrar la información de su establecimiento.
2. **Perfil empresarial y directorio digital:** permite registrar, visualizar y actualizar la información de la agencia, incluyendo nombre comercial, descripción, ubicación, fotografías, datos de contacto y servicios ofrecidos.
3. **Gestor de catálogo turístico:** permite publicar, editar, actualizar y eliminar experiencias turísticas, incorporando información estructurada como destino, descripción, itinerario, duración, precios, horarios, servicios incluidos y condiciones del tour.
4. **Módulo de disponibilidad y programación:** permite configurar y actualizar las fechas, horarios, cupos disponibles y restricciones de cada experiencia turística, facilitando que los turistas consulten información vigente.
5. **Sistema de administración de reservas:** permite recibir, visualizar, aceptar o rechazar solicitudes de reserva, actualizar sus estados y consultar los datos relacionados con cada solicitud.
6. **Centro de notificaciones y seguimiento:** genera avisos sobre nuevas solicitudes, confirmaciones, cancelaciones y modificaciones de reservas, facilitando la comunicación y el seguimiento de las actividades de la agencia.
7. **Panel de analítica y estadísticas:** permite consultar indicadores relacionados con el desempeño de la agencia, como cantidad de experiencias publicadas, consultas recibidas, solicitudes de reserva y servicios con mayor interés por parte de los turistas.

---

## Seguimiento de implementación — 23 de noviembre de 2026

Las secciones anteriores conservan la visión del producto. Este seguimiento
distingue lo implementado en el backend de lo que todavía requiere otra etapa.
La primera entrega fue de backend. Ahora existe una interfaz web React/TypeScript
en `frontend/`, basada en las pantallas de `docs/diseno/`. Consume la API real;
no utiliza datos ficticios en ejecución. Véase `frontend/README.md`.

### Decisiones confirmadas

- Desarrollo del **backend por etapas**.
- **Cloudinary** para los videos de los tours.
- **Mercado Pago en entorno de pruebas** para la etapa de pagos. No realizar cobros reales.
- Notificaciones **internas y por correo SMTP** para la etapa de comunicaciones.

### Primera etapa: seguridad y operación base

- [x] Autenticación HTTP Basic con contraseñas BCrypt persistidas; requiere HTTPS fuera de desarrollo.
- [x] Roles ADMIN, AGENCY_OPERATOR, GUIDE, COORDINATOR y TOURIST.
- [x] Registro público de turistas sin posibilidad de asignarse roles administrativos.
- [x] Alta de cuentas de personal sólo por administradores y bootstrap opcional del primer administrador.
- [x] Operadores limitados a tours, horarios, guías y reservas de su agencia.
- [x] Turistas limitados a sus datos, sus reservas y sus reseñas.
- [x] Guías pueden completar reservas únicamente de salidas que tienen asignadas.
- [x] Protección CSRF conservada. Catálogos y disponibilidad accesibles sin autenticación.
- [x] Video por tour: autorización de carga directa firmada, verificación contra Cloudinary,
  consulta de URL de reproducción y eliminación con invalidación de caché.
- [x] Límites de video: 50 MiB, 120 segundos, formatos MP4/WebM/MOV y dimensiones de video válidas.
- [x] No se aceptan URLs arbitrarias ni metadatos del cliente para publicar un video.
- [x] Endpoints de reseñas, actualización/eliminación de horarios y eliminación de tours.
- [x] Disponibilidad descuenta reservas pendientes tanto al filtrar como al mostrar cupos;
  excluye salidas pasadas y tours/agencias inactivos.
- [x] Versionado optimista de reservas y horarios; bloqueo por turista al crear reservas.
- [x] Driver PostgreSQL de ejecución y conexión por variables de entorno; perfil local explícito.
- [x] Datos ampliados del tour: itinerario, condiciones y prácticas declaradas de sostenibilidad.
- [x] Búsqueda global paginada por precio, duración, destino, dificultad, agencia, categoría y sostenibilidad;
  comparación de 2 a 6 tours activos de distintas agencias.
- [x] Coordenadas de agencia y enlace HTTPS a una fotografía ya alojada en Cloudinary.
- [x] Voucher JSON, QR PNG descargable y check-in de uso único para el día de la salida confirmada.
- [x] Notificaciones internas privadas y cola de correo persistida atómicamente con la reserva.
- [x] Despachador SMTP configurable con reintentos (máximo 5); desactivado por defecto.
- [x] Reporte REST de cancelaciones e inasistencias por tipo de experiencia.

La integración de Cloudinary necesita credenciales y un preset firmado configurados
por el dueño de la cuenta. Las pruebas automatizadas simulan las respuestas del
proveedor: no equivalen a una carga real validada en una cuenta externa.

### Matriz del producto

| Funcionalidad | Estado actual | Pendiente |
|---|---|---|
| Vitrina y mapa | Catálogo, directorio, Leaflet configurable y búsqueda por radio con consentimiento | Configurar proveedor cartográfico y coordenadas reales. |
| Comparador | Búsqueda global y comparación visual de 2–6 tours de distintas agencias | Validación externa de las prácticas declaradas. |
| Pagos | Checkout sandbox, verificación por API, HMAC, bandeja idempotente, reintentos y conciliación | Configurar/validar sandbox; producción/split/reembolsos desde la app no habilitados. |
| Voucher QR | Voucher visual, impresión a PDF del navegador, PNG y canje autorizado sin ID | Configurar zona operativa; no hay PDF firmado de servidor. |
| Reseñas | Gestión propia, listado público, promedio y moderación ADMIN con motivo | Revisión operativa de comentarios reales. |
| Acceso de agencias | Sesiones HttpOnly, logout, revalidación, recuperación por correo y paneles | Configurar SMTP; sesiones compartidas si hay varias réplicas. |
| Perfil empresarial | Panel de datos, coordenadas y fotografía por URL Cloudinary | Carga directa de fotografías y galería. |
| Catálogo | CRUD visual, videos/galería firmados y filtros combinados con fecha/personas | Validar Cloudinary y contenido real. |
| Programación | CRUD, bloqueo de guía y cruces de recorridos que comienzan días anteriores | Validación operativa con agenda real. |
| Reservas | Flujo visual, cupos, historial, expiración opcional y pago independiente asociado | Decidir TTL (por defecto desactivado); auditoría adicional de notas/check-in. |
| Notificaciones | Bandeja privada, cola SMTP, reintentos y panel ADMIN para reponer fallos | Configurar SMTP y verificar entrega real. |
| Analítica | Panel de reportes, ocupación, productividad, inasistencias y cancelaciones por tipo | Medición de visitas/interés. |

### Etapas siguientes

1. **Validar integraciones configuradas:** carga y eliminación real de video con su cuenta
   Cloudinary y entrega SMTP en un buzón de prueba. No se necesitan secretos en el repositorio.
2. **Validar Mercado Pago sandbox implementado:** credenciales, webhooks, compra de
   prueba y conciliación externa. No activar cobros productivos.
3. **Despliegue y endurecimiento:** migraciones versionadas para bases existentes,
   limitación de solicitudes/cargas, limpieza de cargas abandonadas, monitoreo y políticas de retención.
4. **Ampliaciones posteriores:** métricas con consentimiento, carga directa de fotos
   de perfil de agencia y operación multirréplica. Galería de tours, cercanía,
   recuperación y expiración configurable ya están implementadas.
5. **Validar la interfaz web existente:** recorridos completos con el backend y datos reales,
   permisos de cámara y accesibilidad. No confundir pruebas de navegador con dobles HTTP
   con una validación de PostgreSQL o proveedores externos.

El backend de QR y notificaciones está implementado y cubierto por pruebas. No se
debe confundir un adaptador SMTP/Cloudinary probado con dobles HTTP con una entrega
externa real verificada. Los paneles y el código de pagos sandbox existen;
la validación con la cuenta externa del propietario sigue pendiente.

### Ampliación: QR, reseñas e historial

- `POST /api/reservations/check-in`: resuelve el QR y lo canjea en una transacción,
  sin ID manual ni endpoint público de resolución. Conserva CSRF y control de uso único.
- `GET /api/tours/{id}/reviews`: lista pública paginada con promedio global; omite
  IDs de turista/reserva y datos de contacto. No incluye moderación automática.
- `GET /api/reservations/{id}/history`: historial privado de creación y cambios de
  estado, con fecha, estado previo/final, rol y motivo. El correo del actor se guarda
  internamente, pero no se devuelve. No se reconstruyen cambios anteriores.
- Nueva tabla `reservation_history`. Desarrollo: el perfil local puede crearla.
  Bases existentes fuera de local: aplicar la migración aditiva documentada en
  `docs/sql/20261123_reservation_history.sql`, sin borrar datos. No es una línea base
  de todas las migraciones pendientes del proyecto.
- Las pruebas PostgreSQL requieren Docker. Ya se ejecutaron en PostgreSQL temporal
  de Testcontainers durante la entrega posterior de cuentas/pagos; véase la guía operativa.

### Evidencia técnica

- Seguridad: `security/`, `config/SecurityConfig.java`, `api/controllers/AuthController.java`.
- Videos: `media/`, `api/controllers/TourVideoController.java`.
- Reseñas: `api/controllers/ReviewController.java` y `services/impl/ReviewServiceImpl.java`.
- Pruebas de permisos y recorrido con PostgreSQL: `src/test/java/com/edu/unimag/rodatours/security/`.
- Pruebas del adaptador y reglas de video: `src/test/java/com/edu/unimag/rodatours/media/`.
- Configuración y ejemplos: `docs/documentos/seguridad_y_videos.md`.

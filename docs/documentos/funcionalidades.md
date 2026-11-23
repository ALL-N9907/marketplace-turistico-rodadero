# Funcionalidades del Prototipo: plataforma de agencias de turismo unificada para sector específico del rodadero

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
No existe una interfaz web en este repositorio.

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
| Vitrina y mapa | API de catálogo y coordenadas disponibles | Búsqueda por distancia e interfaz con mapa. |
| Comparador | Búsqueda global por rangos y sostenibilidad; comparación de 2–6 tours | Interfaz visual y validación externa de las prácticas declaradas. |
| Pagos | Proveedor elegido: Mercado Pago en pruebas | Modelo de pago, checkout, eventos firmados, idempotencia y conciliación. |
| Voucher QR | Comprobante JSON, imagen PNG y canje único con autorización | Presentación visual/PDF y política explícita de zona horaria. |
| Reseñas | API y servicio disponibles, sólo dueño y reserva completada | Listado público por tour, promedio de calificación y moderación. |
| Acceso de agencias | Cuentas y autorización por agencia implementadas | Panel visual, recuperación de contraseña y administración completa del ciclo de cuentas. |
| Perfil empresarial | Datos, búsqueda por nombre/dirección, coordenadas y URL de fotografía Cloudinary | Carga directa de fotografías y galería. |
| Catálogo | CRUD, videos Cloudinary, itinerario, condiciones, sostenibilidad y filtros ampliados | Interfaz, moderación y búsqueda por disponibilidad combinada con todos los filtros. |
| Programación | Crear/consultar/actualizar/eliminar horarios y asignar guías | Validar traslapes de guías en recorridos de varios días y concurrencia entre asignaciones. |
| Reservas | Flujo de estados, control de cupos y autorización | Expiración acordada de pendientes, historial completo y vinculación con pagos. |
| Notificaciones | Bandeja privada y correo persistente con reintentos | Configurar SMTP y probar entrega real; monitoreo/reenvío de fallos finales. |
| Analítica | Ocupación, productividad, inasistencias y cancelaciones por tipo | Medición de visitas/interés y panel visual. |

### Etapas siguientes

1. **Validar integraciones configuradas:** carga y eliminación real de video con su cuenta
   Cloudinary y entrega SMTP en un buzón de prueba. No se necesitan secretos en el repositorio.
2. **Mercado Pago en pruebas:** estados de pago independientes de la reserva,
   comprobación del pago desde el proveedor, firma de notificaciones y eventos idempotentes.
3. **Despliegue y endurecimiento:** migraciones versionadas para bases existentes,
   limitación de solicitudes/cargas, limpieza de cargas abandonadas, monitoreo y políticas de retención.
4. **Ampliaciones del backend:** visitas/interés, galería de fotos, búsqueda por distancia,
   recuperación de cuentas y condiciones de expiración de reservas pendientes.
5. **Interfaz web:** vitrinas, mapa, reproductor y paneles; no forma parte de esta primera entrega.

El backend de QR y notificaciones está implementado y cubierto por pruebas. No se
debe confundir un adaptador SMTP/Cloudinary probado con dobles HTTP con una entrega
externa real verificada. Pagos y paneles siguen pendientes, no simulados como terminados.

### Evidencia técnica

- Seguridad: `security/`, `config/SecurityConfig.java`, `api/controllers/AuthController.java`.
- Videos: `media/`, `api/controllers/TourVideoController.java`.
- Reseñas: `api/controllers/ReviewController.java` y `services/impl/ReviewServiceImpl.java`.
- Pruebas de permisos y recorrido con PostgreSQL: `src/test/java/com/edu/unimag/rodatours/security/`.
- Pruebas del adaptador y reglas de video: `src/test/java/com/edu/unimag/rodatours/media/`.
- Configuración y ejemplos: `docs/documentos/seguridad_y_videos.md`.

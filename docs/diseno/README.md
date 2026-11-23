# Rodatours · Pantallas para Figma

**34 pantallas SVG editables**, una galería local y una guía de prototipado.
No se ha creado ni modificado un archivo remoto de Figma: esta sesión no tiene
acceso a esa herramienta. Los SVG son el material de importación, **no un `.fig`**.
No contienen componentes nativos, Auto Layout ni conexiones de prototipo.

## Ver e importar

1. Abre [`index.html`](index.html) en un navegador para revisar todas las pantallas.
2. En Figma Design crea un archivo **Rodatours · Producto**.
3. Crea páginas: `00 Sistema visual`, `01 Viajero`, `02 Agencia`, `03 Guía`,
   `04 Administración`, `05 Coordinación`, `06 Estados`, `07 Móvil`, `08 Futuro`.
4. Arrastra los archivos de [`pantallas/`](pantallas/) al lienzo de cada página.
   Si tu cliente no permite arrastrarlos, usa su opción de importar o pegar SVG.
5. Conserva sus dimensiones: escritorio **1440 × 1000**, móvil **390 × 844**.
   Usa frames contenedores del mismo tamaño si Figma importa los SVG como grupos.
6. Instala Inter si deseas usarla; las alternativas son Segoe UI y Arial.
   Revisa la importación de textos: según el cliente pueden requerir recreación.
7. Convierte botones, campos, tarjetas y estados en componentes nativos;
   aplica Auto Layout y conecta los flujos de abajo en la pestaña Prototype.

No se utiliza un plugin, no se requieren secretos y no se envían datos a terceros.
Las ilustraciones son vectoriales originales sin fotografías externas.
El mapa es esquemático; nombres, datos personales, cifras y coordenadas son ficticios.
El QR se representa con un espacio reservado, deliberadamente no canjeable.

## Fuentes y precedencia

- [`funcionalidades.md`](../documentos/funcionalidades.md): seguimiento del
  23/11/2026, decisiones confirmadas y matriz del estado del producto.
- [`seguridad_y_videos.md`](../documentos/seguridad_y_videos.md): permisos,
  autenticación, integraciones, catálogo, QR y notificaciones actuales.
- [`marketplace_turistico_rodadero.md`](../documentos/marketplace_turistico_rodadero.md):
  entidades, atributos y reglas del recorrido de reserva.
- [`api_controllers.md`](../documentos/api_controllers.md): contratos iniciales;
  sus notas antiguas de seguridad y reseñas quedan superadas por las dos primeras fuentes.

La disponibilidad **sí considera solicitudes pendientes**, según el seguimiento
actual; no se mantiene como abierta la duda del documento inicial. Confirmar sigue
siendo una acción del operador o administrador, no del turista.

La documentación no contiene una identidad visual aprobada. La paleta Caribe sereno,
el nombre gráfico rodatours, la tipografía y la composición son **propuestas de diseño**.
Los controles y menús muestran destinos propuestos, no operaciones funcionales.

## Inventario y ubicación en Figma

| Página | Pantallas (prefijos de archivo) |
|---|---|
| Sistema visual | 34 sistema visual y componentes |
| Viajero | 01 explorar, 02 filtros, 03 detalle, 04 comparar, 05 agencias/mapa, 06 acceso, 07 registro, 08 solicitud, 09 reservas, 10 voucher, 11 cancelar, 12 reseña, 24 avisos |
| Agencia | 13 resumen, 14 catálogo, 15 edición/video, 16 agenda, 17 reserva, 18 perfil, 31 formulario de guía |
| Guía | 19 salida y cierre, 20 validar QR |
| Administración | 21 agencias, 22 cuentas de personal, 30 cierre, 32 turistas, 33 categorías |
| Coordinación | 23 reportes |
| Estados | 29 carga, vacío, validación, conflicto, prohibido y proveedor no disponible |
| Móvil | 26 explorar, 27 solicitar, 28 voucher |
| Futuro | 25 Mercado Pago sandbox, sin acción habilitada |

`inventario.json` registra los archivos y sus dimensiones. Las pantallas de escritorio
son vistas clave de altura fija, no representaciones de todo el scroll de cada página.
Las tres móviles desarrollan el recorrido principal; no implican que todos los paneles
tengan ya una variante móvil diseñada.

## Flujos que deben conectarse en Figma

| Flujo | Conexiones propuestas |
|---|---|
| Explorar y solicitar | 01 → 02 → 03 → 08 → 09; sin cuenta, 08 → 06 → 07 → 08 |
| Comparar | Selección de 2–6 tours de distintas agencias en 02 → 04 → 03 |
| Directorio | 01 → 05 → 02, con filtro de agencia |
| Seguimiento | 09 → 10 sólo para reserva confirmada; pendiente abre detalle con estado real |
| Cancelación | 09 → 11 → confirmación explícita → 09 con estado cancelado |
| Reseña | Reserva completada en 09 → 12 → confirmación de publicación |
| Operación de agencia | 13 → 17 → confirmar o cancelar → respuesta real y refresco |
| Catálogo y agenda | 14 → 15; 14 → 16; 18 → 31 |
| Guía | 19 → 20 → resultado de canje; volver a 19 para completar, por separado |
| Administración | 21/32/33 → formularios; 22 alta de personal; 30 cierre de reserva |
| Coordinación | 23 → consultar por rango → resultados de cada reporte |
| Avisos | 24 → detalle autorizado de reserva; marcar leído sin salir de la bandeja |
| Móvil | 26 → detalle adaptado pendiente de diseñar → 27 → lista adaptada pendiente → 28 tras confirmación |

En el prototipo, usa clic/tap y transiciones sencillas de 150–200 ms. Crea overlays
de confirmación para cancelación, eliminación de video/tour y cierre de reserva.
No conectes “solicitar” directamente a “confirmada”, ni check-in a “completada”.
Los overlays y resultados individuales aún deben construirse como frames nativos;
la pantalla 29 proporciona su lenguaje visual, no una cobertura de todas las variantes.

## Estados y permisos

| API | Texto de interfaz | Acciones visibles |
|---|---|---|
| SCHEDULED | Pendiente de confirmación | Confirmar: operador/ADMIN. Cancelar: dueño, operador/ADMIN. |
| CONFIRMED | Confirmada | Cancelar: dueño, operador/ADMIN. Completar: guía asignado/ADMIN. No asistió: ADMIN. |
| CANCELLED | Cancelada | Consulta; sin reabrir. |
| COMPLETED | Completada | Consulta; reseña para el turista dueño. |
| NO_SHOW | No asistió | Consulta; sin reabrir. |

- Check-in: ADMIN, operador propietario o guía asignado; reserva confirmada,
  salida hoy, uso único. No cambia el estado ni los cupos.
- El cierre no está permitido antes de la fecha de salida. Validar en el servidor.
- Los listados se limitan al alcance de cada rol. El operador consulta reservas por
  una salida propia, no mediante un listado global de toda la agencia.
- No se presume un listado de todas las reservas para guías. La pantalla 19 es un
  detalle de salida/reserva conocida, no una nueva consulta global autorizada.
- Sólo ADMIN administra turistas y cuentas de personal. El registro público crea TOURIST.
- Los reportes globales pertenecen a ADMIN/COORDINATOR; un operador sólo consulta
  ocupación de su agencia.
- Cancelar una pendiente no debe aumentar cupos físicos nunca descontados. La API
  aplica la liberación correcta; la interfaz sólo refresca el resultado del servidor.

## Correspondencia con backend y límites

| Pantallas | Contrato o condición |
|---|---|
| 01–04 | `/api/catalog/tours`, `/compare`, tours y detalles. La agenda se consulta aparte; no presentar filtros combinados con disponibilidad. |
| 05, 18 | Agencias y `/location`. Coordenadas sin búsqueda por distancia. Foto por URL HTTPS de Cloudinary ya alojada, sin carga directa. |
| 06–07, 22 | `/api/auth/csrf`, `/me`, `/register`, `/accounts`. HTTP Basic y CSRF, no JWT ni recuperación de contraseña. |
| 08–11, 17, 30 | Disponibilidad y reservas; cantidad positiva, sin solapamientos, precio y fechas derivados por servidor. |
| 10, 20, 28 | `/voucher`, `/voucher/qr`, `/check-in`. Sólo QR PNG, sin prometer PDF. |
| 12 | Reseñas propias y reserva completada. Sin promedio público, moderación o listado público por tour. |
| 13, 23 | Ocupación, productividad, inasistencias y cancelaciones por tipo. Productividad cuenta reservas completadas, no salidas únicas. |
| 14–16 | Tours, detalles, horarios y video. Separar guardar datos, guardar detalles y publicar video. |
| 18, 31 | Guías de la agencia. Crear un guía no crea su cuenta de acceso. |
| 21, 32, 33 | Agencias, turistas y categorías. Tipos de experiencia: sólo alta y consulta según contrato documentado. |
| 24 | `/api/notifications`, `/{id}/read`; bandeja propia paginada. |
| 25 | Futuro: Mercado Pago sólo sandbox, sin checkout ni cobros reales. |

### Integraciones y consultas que no deben simularse como terminadas

- Mapa: requiere elegir proveedor cartográfico para la implementación web. En los SVG
  no hay geocodificación, ubicaciones verificadas ni búsqueda por distancia.
- Video: autorización firmada → carga directa → verificación del servidor → publicación.
  Mostrar progreso, verificación pendiente, rechazo por tamaño/duración y fallo 503.
- El resumen del operador es una composición propuesta de consultas autorizadas,
  no un endpoint de dashboard agregado. No incluye ventas, ingresos o visitas.
- Guía: descubrimiento automático de todas sus salidas y lectura de cámara requieren
  resolver la integración de interfaz y verificar contratos; no se amplían permisos.
- Notificaciones: bandeja disponible; SMTP requiere configuración y entrega real
  verificada. No hay panel de reenvío de fallos implementado.
- Voucher: conservar la fecha operativa del servidor; acordar zona horaria antes
  de despliegue. No asumir una política de zona horaria ya implementada.
- Pagos y expiración de pendientes siguen sin resolverse en el backend; no añadir
  cuentas regresivas ni garantías de cobro/reserva.

## Accesibilidad y comportamiento para implementación

- Etiquetas persistentes, foco visible y mensajes de error junto al campo;
  estados con texto además de color. Objetivo: WCAG 2.2 AA, pendiente de auditoría.
- Controles principales de 44 px o más, navegación con teclado y orden lógico.
- No colocar contraseñas en URL, archivos de diseño ni almacenamiento persistente
  del navegador. No mostrar hash ni secretos de Cloudinary/SMTP.
- Durante escrituras, deshabilitar doble envío y esperar respuesta antes de mostrar éxito.
- 400: errores por campo o regla; 401: pedir acceso preservando selección no sensible;
  403: sin permiso; 404: recurso no disponible; 409: refrescar estado/cupos;
  503: fallo de proveedor, ofrecer reintento sin afirmar publicación.
- Confirmaciones de acciones destructivas con alternativa clara para volver.
- No publicar datos privados en vistas públicas ni usar datos personales en el QR.

## Regenerar

```powershell
node docs/diseno/generar-pantallas.mjs
```

El script usa sólo Node.js y su biblioteca estándar. Regenera los SVG, el inventario
y la galería; **sobrescribe esos archivos generados**, no documentos ni código Java.
Para conservar ajustes manuales, modifica el generador o guarda las variantes aparte.
No modifica el backend ni incorpora una interfaz ejecutable al proyecto Spring Boot.

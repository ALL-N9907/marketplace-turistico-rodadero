# Rodatours · Frontend

Aplicación web responsive basada en las [pantallas para Figma](../docs/diseno/README.md).
Incluye vistas públicas, recorrido del viajero y paneles para ADMIN,
AGENCY_OPERATOR, GUIDE y COORDINATOR.

**No usa datos de demostración en ejecución.** Si la API está apagada, muestra el
error de conexión. Los dobles HTTP viven exclusivamente en pruebas. No se añadió
un backend paralelo ni se modificó la seguridad de Spring Boot.

## Ejecutar

Requisitos: Node.js 22.12+ (recomendado 24 LTS), npm, Java 21 y PostgreSQL para el backend.

```powershell
cd frontend
npm ci
Copy-Item .env.example .env.local
npm run dev
```

Abre `http://127.0.0.1:5173`. Usa siempre el mismo hostname para conservar las cookies
CSRF. El proxy de Vite dirige `/api` a `http://localhost:8080`; puedes cambiar
`API_PROXY_TARGET` en `.env.local`. **No** debes poner contraseñas de base de datos,
SMTP, Cloudinary o Mercado Pago en variables `VITE_*` (son públicas).

Arranca Spring Boot en otra terminal, desde la raíz del repositorio:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/rodatours"
$env:DB_USERNAME = "rodatours"
$env:DB_PASSWORD = "<contraseña>"
$env:ADMIN_EMAIL = "<correo del primer administrador>"
$env:ADMIN_PASSWORD = "<contraseña de al menos 12 caracteres y máximo 72 bytes UTF-8>"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

El bootstrap no modifica ni eleva cuentas existentes. Retira las variables ADMIN_*
después de crear el primer administrador. No hay usuarios ni contraseñas predeterminados.
El perfil local modifica el esquema para desarrollo; producción requiere migraciones.

## Qué está desarrollado

| Área | Funciones conectadas a la API |
|---|---|
| Catálogo | Filtros por destino, agencia, categoría, precio, duración, dificultad y prácticas declaradas; paginación; comparación de 2–6 tours de agencias distintas. |
| Experiencia | Detalle, itinerario, condiciones, servicios, video publicado, consulta de disponibilidad por fecha y personas; creación de solicitud. |
| Acceso | Registro público de turista, inicio de sesión por HTTP Basic, rutas por rol y cierre de acceso local. |
| Viajero | Listado propio por estado, detalle, cancelación con motivo, voucher privado, descarga de QR PNG, alta/edición/eliminación de reseñas propias. |
| Agencia | Gestión de tours, detalles y sostenibilidad; carga firmada, verificación y eliminación de video; CRUD de horarios; asignación de guía mediante su ID; consultas de reservas por salida propia; confirmación/cancelación; perfil y coordenadas/foto por URL. |
| Guía | Consulta de una reserva conocida y autorizada; lectura de QR por cámara o imagen; canje explícito y cierre como completada. |
| Administración | Agencias, turistas, guías, categorías (alta/consulta), cuentas de personal, creación administrativa de reservas y cierre/no-show. |
| Coordinación | Ocupación, productividad, turistas con inasistencias y cancelaciones por tipo; tabla y gráfica de productividad. |
| Comunicaciones | Bandeja privada paginada y marcado como leído. |

El mapa tiene integración Leaflet pero no solicita teselas hasta configurar URL y
atribución. El lector QR no realiza el canje al escanear: requiere confirmación explícita.
El estado y la fecha para cada transición se verifican siempre en Spring Boot.

### Límites deliberados

- Mercado Pago aparece como etapa futura; no hay checkout ni cobros reales.
- No hay recuperación de contraseña, modificación pública de perfil del turista,
  promedio de reseñas, galería, PDF del voucher ni expiración automática de pendientes.
- No se inventa una consulta general de reservas/salidas asignadas a guías. Se abre
  por ID o se introduce el ID de reserva junto con el payload leído del QR. El payload
  actual sólo contiene el código QR, no el ID numérico: no es posible inferirlo sin
  ampliar el contrato. La agencia debe facilitar el ID junto con el voucher.
- Algunas pantallas operativas solicitan IDs reales de agencia, turista, guía o salida;
  pueden consultarse en sus tablas administrativas. No hay búsqueda remota de personas.
- El resumen obtiene conteos del catálogo autorizado, no de un endpoint de dashboard.
  No presenta ingresos, visitas ni estadísticas de interés inexistentes.
- Formularios CRUD sencillos utilizan validación HTML y muestran errores del servidor;
  acceso y registro usan React Hook Form/Zod. Los permisos visuales no sustituyen la API.
- No hay historial de cambios completo ni mecanismos de reenvío de correo desde la UI.

## Arquitectura

- React 19, TypeScript, Vite y React Router.
- TanStack Query para solicitudes, caché, estados y refresco después de mutaciones.
- Fetch centralizado en `src/lib/api.ts`; todas las rutas API son relativas a `/api/`.
- React Hook Form/Zod, Lucide, Leaflet, Recharts y html5-qrcode.
- CSS propio con tokens de la propuesta visual, componentes reutilizables y variantes
  responsive. No se añadieron Tailwind/shadcn a la vez que una segunda capa de estilos.
- Las tipografías usan alternativas del sistema; no hay llamadas a Google Fonts ni
  dependencia de una CDN para renderizar la interfaz.
- `src/pages/`: módulos de dominio; `src/components/`: estructura, formularios, estados
  y componentes comunes; `src/types.ts`: DTOs de la API existente.

### Autenticación y datos privados

Las credenciales Basic se retienen **sólo en memoria**, se envían únicamente a `/api/`
y desaparecen al salir o recargar. Esto es deliberado: el backend no ofrece todavía
un contrato de inicio/cierre de sesión persistente para una SPA. No se afirma revocar
sesiones del servidor; para persistencia segura hay que acordar sesiones HttpOnly/BFF
o un mecanismo de autenticación renovable con su cierre y expiración.

Cada escritura solicita un token `/api/auth/csrf` y conserva su cookie. No se desactiva
CSRF ni se reintentan automáticamente mutaciones. Las imágenes QR se descargan con
Fetch autenticado, se muestran con una URL de objeto temporal y se liberan al salir.
La caché de TanStack Query se limpia al cambiar/cerrar acceso; no se persiste en disco.
No se guarda ningún dato privado en `localStorage` ni `sessionStorage`.

### Video

El flujo es autorización del backend → `multipart/form-data` directo a Cloudinary
sin cookies ni credenciales Basic → `/complete` sin metadatos del cliente → URL
verificada. El cliente valida extensión/tamaño; el servidor valida duración y el
resto de metadatos. Si la carga falla, permite verificar de nuevo o eliminar la
sesión pendiente. No afirma un porcentaje de progreso inexistente.

## Pruebas y compilación

```powershell
npm test
npm run build
npx playwright install chromium
npm run test:e2e
```

Si tienes Google Chrome instalado puedes ejecutar las pruebas sin descargar Chromium:

```powershell
$env:PLAYWRIGHT_CHANNEL = "chrome"
npm run test:e2e
```

Vitest cubre cliente HTTP, CSRF, negociación PNG, errores y permisos visuales.
Playwright prueba navegación, acceso protegido, solicitud sin sesión y errores de API
en escritorio y móvil, además de solicitud autenticada con CSRF y consulta acotada
del operador, mediante dobles HTTP. **No equivalen a pruebas contra PostgreSQL,
Cloudinary, SMTP ni cuentas reales.**

Verificación de esta entrega: compilación de producción correcta, **25 pruebas
unitarias y 12 pruebas de navegador** correctas (Chrome instalado, escritorio y
emulación móvil). Se revisaron capturas del catálogo en ambos tamaños.

`npm run build` genera `dist/`. `npm run preview` permite revisar la compilación,
pero su servidor no sustituye el proxy de producción para `/api`.

## Despliegue

Publica `dist/` con un servidor estático y reenvía `/api/*` a Spring Boot bajo el
**mismo dominio HTTPS**. Ver ejemplo [`deploy/nginx.conf`](deploy/nginx.conf).
Conserva cookies y cabeceras Authorization/CSRF. Para rutas de React, sirve `index.html`
como fallback; nunca devuelvas ese HTML para una ruta `/api` inexistente.

No sirve directamente desde Spring en su configuración actual: su política exige
autenticación para rutas no API. El ejemplo Nginx evita cambiar esa política.
No expongas el puerto de Spring al público sin la protección de red apropiada.

## Lista de conexiones y configuración a tu cargo

### Necesario para la operación base

1. **PostgreSQL y esquema:** crear la base, configurar DB_URL/DB_USERNAME/DB_PASSWORD
   en Spring Boot y ejecutar migraciones si ya existe una base de datos. Hacer copias.
2. **Backend y proxy:** levantar Spring en el puerto indicado por API_PROXY_TARGET;
   en producción, dominio/SSL, proxy `/api`, fallback de rutas SPA y alojamiento de `dist/`.
3. **Cuenta administrativa y datos reales:** crear el primer ADMIN; registrar agencias,
   categorías, turistas, guías, tours y horarios. Crear cuentas de operadores/guías
   vinculadas a las agencias y guías correctos. Sin datos, los catálogos estarán vacíos.
4. **Zona horaria operativa:** acordar/configurar la del servidor (p. ej.
   `America/Bogota`) y verificar reservas cercanas a medianoche; la API usa hora local
   sin offset y el navegador la muestra como fecha/hora local, no como instante UTC.

### Para habilitar integraciones ya preparadas

5. **Cloudinary:** configurar CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY,
   CLOUDINARY_API_SECRET y CLOUDINARY_VIDEO_PRESET sólo en el backend. Crear preset
   firmado y probar carga/verificación/eliminación real. Fotos: alojarlas antes y
   registrar su URL HTTPS admitida; no hay cargador de imágenes en esta entrega.
6. **Correo:** configurar MAIL_ENABLED, SMTP_HOST, SMTP_PORT, SMTP_USERNAME,
   SMTP_PASSWORD, SMTP_FROM, SMTP_AUTH y SMTP_STARTTLS en Spring; verificar entrega
   en un buzón de prueba. La bandeja interna funciona aunque SMTP esté apagado.
7. **Mapa:** contratar/elegir proveedor de teselas, configurar VITE_MAP_TILE_URL y
   VITE_MAP_ATTRIBUTION y registrar coordenadas reales. Revisar licencia, cuotas,
   privacidad y restricciones de cualquier clave pública por dominio.
8. **Cámara:** publicar por HTTPS y otorgar permisos del navegador; probar en equipos
   reales. Para un escaneo autónomo sin introducir ID, falta un endpoint autorizado
   de resolución QR→reserva o una revisión acordada del formato del voucher.

### Requiere desarrollo adicional, no sólo conectar credenciales

9. **Mercado Pago sandbox:** implementar el modelo de pagos, checkout, verificación
   del proveedor, webhooks firmados, idempotencia y conciliación; después integrar
   su SDK de frontend. No poner un botón de “pagar” que confirme por sí solo la reserva.
10. **Autenticación persistente/recuperación:** acordar e implementar sesiones seguras,
    expiración, cierre en servidor y recuperación de cuentas si las necesitas.
11. **Producción:** completar migraciones, monitoreo de SMTP/cargas abandonadas,
    límites de solicitudes, política de pendientes, retención y documentos legales;
    validar permisos, concurrencia, accesibilidad y recorridos con datos reales.

No debes conectar manualmente cada botón: los endpoints disponibles ya están
consumidos por el frontend. Esta lista distingue infraestructura/configuración de
funciones que aún no existen en el backend.

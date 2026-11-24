# Rodatours: entrega de código y puesta en marcha

Esta guía separa **código desarrollado**, **configuración del propietario** y
**validación externa pendiente**. No es necesario borrar la base de datos.

## 1. Qué incluye el código actual

| Módulo | Implementación |
|---|---|
| Acceso | Login JSON, cookie de sesión HttpOnly/SameSite, expiración por inactividad, logout en servidor, restauración al recargar y CSRF. HTTP Basic se conserva para clientes de API. |
| Revocación | Cada solicitud con sesión vuelve a comprobar contraseña, rol, agencia, guía y estado. Un cambio invalida la sesión en su próxima solicitud. |
| Recuperación | Token aleatorio de 256 bits, hash SHA-256, uso único, 30 minutos, respuesta genérica y correo en cola con token cifrado AES-GCM. |
| Pagos | Checkout Pro hospedado, exclusivamente sandbox, importe de la reserva, webhook HMAC, bandeja idempotente, verificación por API, reintentos y conciliación manual. |
| Operación de pagos | Estado independiente de la reserva; detecta pagos aprobados duplicados, moneda/vendedor/importes incorrectos y respuestas inciertas del proveedor. |
| Fotografías de tours | Hasta seis imágenes, carga directa firmada, verificación remota, galería y eliminación con invalidación del CDN. |
| Moderación | ADMIN puede ocultar/publicar reseñas con motivo; las ocultas no aparecen públicamente ni cuentan en el promedio. |
| Entregas | ADMIN consulta avisos SMTP y eventos de pago, y repone fallos en las colas. No reenvía avisos que ya constan enviados. |
| Reservas | Historial privado y expiración opcional de pendientes, con liberación de retenciones sin aumentar cupos físicos. |
| Programación | Bloqueo de guía durante asignación y validación de recorridos que empezaron en días anteriores. |
| Búsqueda | Fecha/personas combinables con filtros del catálogo; búsqueda de agencias por radio geográfico. |
| Voucher | PNG privado y presentación imprimible; el navegador puede guardar el voucher como PDF. No es una factura ni un PDF firmado por el servidor. |

Los servicios externos están **deshabilitados o fallan con 503 si faltan credenciales**.
No hay aprobaciones, correos entregados ni cargas simuladas en ejecución.

## 2. Tu primera tarea: backend y frontend locales

### Requisitos

- Java 21 y Node.js 22.12+ (recomendado 24).
- PostgreSQL iniciado, base `rodatours` y usuario con permisos sobre su esquema.
- Docker Desktop sólo es necesario para las pruebas automatizadas de PostgreSQL,
  no para usar tu PostgreSQL instalado.

Desde PowerShell:

```powershell
cd C:\Users\ndezz\Desktop\rodatours
.\ops\check-environment.ps1
.\ops\start-local.ps1 -BootstrapAdmin
```

El segundo script pregunta contraseñas de forma oculta, arranca con perfil local y
restaura las variables del proceso al salir. No crea ni borra una base de datos.
Usa `-BootstrapAdmin` sólo si necesitas un administrador nuevo; su correo no debe
pertenecer a otra cuenta. En arranques posteriores:

```powershell
.\ops\start-local.ps1
```

Si Windows bloquea scripts, puedes ejecutar la misma invocación con una excepción
limitada a ese proceso, tras revisar el archivo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ops\start-local.ps1 -BootstrapAdmin
```

No cambies permanentemente la política de ejecución del equipo por este proyecto.
Alternativamente define DB_URL/DB_USERNAME/DB_PASSWORD en la terminal y ejecuta
`mvnw.cmd` como se documenta en `seguridad_y_videos.md`.

Cuando aparezca `Started RodatoursApplication`, abre
`http://localhost:8080/api/experience-types`. Una respuesta `[]` es válida.
Si falla, comparte el primer `Caused by`, no únicamente `BUILD FAILURE`, y oculta secretos.

En una segunda terminal:

```powershell
cd C:\Users\ndezz\Desktop\rodatours\frontend
npm ci
npm run dev
```

Abre `http://127.0.0.1:5173`. El proxy de Vite conecta `/api` con el puerto 8080.
Si usas otro puerto, configura `API_PROXY_TARGET` en `frontend/.env.local` y reinicia
Vite. Spring Boot **no lee archivos `.env` automáticamente**: sus secretos deben ser
variables del proceso, del servicio o del gestor de secretos de tu alojamiento.

### Datos iniciales y prueba base

1. Inicia sesión como ADMIN.
2. Crea agencia, tipo de experiencia, guía, tour activo y salida futura con cupos.
3. Crea cuentas de operador/guía vinculadas correctamente. Sus IDs aparecen en las tablas.
4. Registra un turista desde el registro público y solicita una reserva.
5. Confírmala como administrador u operador.
6. Para validar QR, usa una salida con fecha **hoy**. El segundo canje debe rechazarse.
7. Comprueba que el turista no acceda a reservas ajenas y que el guía sólo cierre las asignadas.

No necesitas configurar pagos, SMTP ni Cloudinary para este recorrido base.

## 3. Actualizar una base con datos

Antes de actualizar: copia de seguridad desde pgAdmin y detén el backend.

- Perfil **local**, sólo desarrollo: Hibernate puede crear las estructuras nuevas.
- Perfil normal con `ddl-auto=validate`: aplicar los scripts aditivos necesarios,
  con el propietario del esquema, **antes de arrancar el backend nuevo**:
  1. `docs/sql/20261123_reservation_history.sql`, si no existe `reservation_history`.
  2. `docs/sql/20261123_accounts_payments_gallery.sql`, para cuentas/pagos/fotos/moderación.

No ejecutes el primer script si local ya creó esa tabla. El segundo contempla
estructuras existentes, pero debes revisar el resultado y conservar el respaldo.
Los scripts presuponen el esquema base actual: no solucionan automáticamente
migraciones antiguas omitidas. No se ha instalado Flyway ni habilitado un baseline
automático sobre tu base. **No utilices DROP DATABASE para actualizar código.**

## 4. SMTP y recuperación de contraseñas

### Lo que debes obtener

Una cuenta SMTP y un remitente verificado. Puede ser un proveedor transaccional o
un buzón de pruebas. Sigue las instrucciones de autenticación de tu proveedor;
si pide contraseña de aplicación, no uses la contraseña normal de tu correo.

Configura en el **backend**:

| Variable | Valor esperado |
|---|---|
| MAIL_ENABLED | `true` para enviar; predeterminado `false`. |
| SMTP_HOST / SMTP_PORT | Servidor y puerto del proveedor; habitual 587 con STARTTLS. |
| SMTP_USERNAME / SMTP_PASSWORD | Credenciales SMTP. |
| SMTP_FROM | Remitente autorizado. |
| SMTP_AUTH / SMTP_STARTTLS | Habitualmente `true`; verificar con el proveedor. |
| PASSWORD_RESET_ENABLED | `true` después de configurar SMTP y la clave de cifrado. |
| RESET_ENCRYPTION_KEY | 32 bytes aleatorios codificados en Base64, secreto exclusivo del servidor. |
| APP_PUBLIC_URL | Origen público del frontend. HTTPS fuera de desarrollo. |

Genera la clave **en tu terminal**, no en el chat:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:RESET_ENCRYPTION_KEY = [Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Guárdala en tu gestor de secretos para siguientes arranques. Si la pierdes/cambias,
los correos pendientes cifrados con la clave anterior no podrán enviarse. Los enlaces
ya enviados siguen sujetos a su hash, vencimiento y uso único.

Para probar localmente puedes usar `APP_PUBLIC_URL=http://127.0.0.1:5173` para
recuperación. Mercado Pago requiere una URL HTTPS pública por separado.

### Prueba de aceptación

1. Reinicia el backend con esas variables.
2. Pulsa **Olvidé mi contraseña** e introduce un correo registrado.
3. La respuesta es genérica, exista o no la cuenta. No se expone el token en la API.
4. Espera el correo (el despachador revisa la cola cada 15 segundos).
5. Abre el enlace, cambia la contraseña y prueba iniciar sesión.
6. Verifica que reutilizar el enlace falle y que una sesión anterior deje de funcionar.

El token viaja en el fragmento `#token=`, que el navegador elimina de la barra al
abrir la pantalla. No lo compartas. El correo puede reintentarse hasta cinco veces;
SMTP no garantiza entrega exactamente una vez. No se han enviado correos reales
desde el agente. Los avisos de reservas se monitorizan en **Entregas y moderación**;
la recuperación tiene su cola independiente y puede solicitarse de nuevo.

## 5. Mercado Pago: únicamente pruebas

### Lo que debes obtener

En Mercado Pago Developers crea/configura tu aplicación para Colombia y consigue:

- Un **Access Token de pruebas `TEST-…`**.
- El ID del vendedor/receptor de pruebas (`collector_id`).
- El secreto de Webhooks de la aplicación (no es el Access Token).
- Una URL HTTPS pública que permita llegar a tu backend. Para pruebas locales,
  puedes usar un túnel HTTPS revisado por ti. No expongas PostgreSQL ni pgAdmin.

Variables del backend:

```text
MP_ENABLED=true
MP_ACCESS_TOKEN=<TEST-...>
MP_COLLECTOR_ID=<id numérico del vendedor de pruebas>
MP_WEBHOOK_SECRET=<secreto de notificaciones de al menos 16 caracteres>
APP_PUBLIC_URL=https://<origen del frontend>
MP_NOTIFICATION_URL=https://<origen del backend>/api/payments/webhook
```

No se necesita una clave secreta en el frontend: se utiliza Checkout Pro hospedado.
El código **rechaza tokens productivos y URLs de checkout fuera de hosts sandbox
permitidos**. Si tu modalidad de cuenta sólo ofrece credenciales distintas, verifica
la documentación/capacidades del proveedor: no quites esa protección para probar.

### Configurar notificaciones

En el panel de Webhooks de Mercado Pago, configura la URL de **modo pruebas** y
el tópico **Pagos (`payment`)**. Copia el secreto generado al entorno del backend.
Conserva las cabeceras `x-signature`, `x-request-id` y el parámetro `data.id` a través
del proxy. Sólo ese endpoint excluye CSRF; los botones del usuario siguen protegidos.

El backend valida HMAC-SHA256 antes de encolar. La firma usa el manifiesto
`id:<data.id>;request-id:<x-request-id>;ts:<ts>;`. El contenido del cuerpo y los
parámetros de retorno no acreditan pagos. Se consulta el recurso auténtico por API
y se comprueban `live_mode=false`, vendedor, COP, importe y referencia local.

### Prueba de aceptación

1. Crea y confirma una reserva cuya salida no haya empezado.
2. Abre **Pago sandbox / consultar estado** desde la reserva.
3. Prepara el checkout y abre el enlace de pruebas.
4. Usa exclusivamente compradores/tarjetas de prueba indicados por Mercado Pago.
5. Regresa a la aplicación y pulsa **Verificar con el proveedor**, o espera al webhook.
6. Repite la notificación y verifica que no duplique el efecto ni cambie cupos.
7. Revisa eventos fallidos en **Entregas y moderación → Eventos de pago**.

### Reglas importantes

- La agencia confirma primero; pagar no confirma, cancela ni reabre una reserva.
- El check-in conserva sus reglas de estado/fecha y no exige pago aprobado por
  defecto. No se debe asumir una política de acceso condicionado al pago sin
  acordarla e implementarla expresamente antes de una operación productiva.
- Cancelar una reserva pagada **no emite un reembolso automático**. Los reembolsos
  se gestionan en el proveedor y su estado se consulta; no hay botón para emitirlos.
- Una preferencia válida se reutiliza. No se genera otra automáticamente al vencer.
- Ante un timeout incierto, se conserva `CHECKOUT_UNKNOWN` y se bloquea otro checkout.
  Conciliar y revisar en el proveedor evita un segundo pago. El sistema no adivina éxito.
- Dos aprobaciones distintas para la referencia generan `REVIEW_REQUIRED`.
- Webhooks se guardan antes de responder y se procesan después, con hasta cinco
  intentos. Una firma nueva de más de 10 minutos se rechaza; sincroniza el reloj
  del servidor. Una repetición ya registrada se reconoce sin repetir el cobro.
- La conciliación consulta hasta 20 pagos recientes de la referencia. Si hay más,
  requiere revisión del proveedor; no es una conciliación contable masiva.
- Los tests usan dobles HTTP. **Falta la prueba real en tu cuenta sandbox.** No se
  han creado cuentas, preferencias ni cobros reales desde el agente.

Referencia consultada: https://www.mercadopago.com.co/developers/es/docs/your-integrations/notifications/webhooks

## 6. Cloudinary: videos y fotografías

Configura sólo en backend:

```text
CLOUDINARY_CLOUD_NAME=<nombre de nube>
CLOUDINARY_API_KEY=<clave>
CLOUDINARY_API_SECRET=<secreto>
CLOUDINARY_VIDEO_PRESET=<preset firmado de video>
CLOUDINARY_IMAGE_PRESET=<preset firmado de imagen>
```

Crea **dos presets firmados**, no unsigned:

- Video: MP4/WebM/MOV, máximo 50 MiB; el servidor verifica hasta 120 segundos.
- Imagen: JPEG/PNG/WebP, máximo 5 MiB. Restringe también dimensiones/formato en el
  proveedor; el servidor verifica dimensiones positivas, máximo 12000 por lado y 40 MP.

En el panel de tours abre **Detalles y video**. Puedes cargar fotos en la galería,
verificar cargas pendientes y eliminarlas. No se publica hasta comprobar el activo
con la API administrativa de Cloudinary. Nunca pegues `api_secret` en el frontend.

Prueba cargar, reproducir/visualizar y eliminar un activo real. El CDN puede tardar
en retirar cachés. Las cargas pendientes cuentan en el límite de seis fotos y
pueden eliminarse desde el panel; su autorización dura una hora. No hay limpieza
automática de activos abandonados. Las fotografías de perfil de agencia conservan
el formulario de URL Cloudinary previamente alojada, no un cargador independiente.

Los activos son públicos en Cloudinary: no subas documentos ni datos sensibles.

## 7. Mapa y ubicación

El directorio y la búsqueda por radio funcionan con coordenadas de las agencias,
sin contratar mapas. Se calcula distancia geográfica, no una ruta de conducción.
La ubicación del visitante sólo se solicita tras pulsar **Usar mi ubicación**;
también puede introducir coordenadas manualmente. Se envía por POST protegido con
CSRF y no se persiste. No habilites logs de cuerpos HTTP que incluyan coordenadas.

Para ver teselas en Leaflet, elige un proveedor y configura en `frontend/.env.local`:

```text
VITE_MAP_TILE_URL=https://<proveedor>/{z}/{x}/{y}.png
VITE_MAP_ATTRIBUTION=<atribución exigida por su licencia>
```

Reinicia Vite; para producción reconstruye el frontend. Cualquier clave VITE_* es
pública: aplica restricciones por dominio y cuotas. No pongas secretos ahí.
Verifica condiciones, privacidad y atribución del proveedor elegido.

## 8. Política de pendientes y zona horaria

`RESERVATION_PENDING_TTL_MINUTES=0` conserva el comportamiento anterior: no expira.
Si acuerdas un plazo, configura por ejemplo `60` para una hora (máximo 10080 minutos).
El proceso revisa cada minuto hasta 50 reservas por lote. Sólo cancela SCHEDULED,
registra historial/avisos y libera retenciones, no stock descontado.
**Decide y comunica esa política antes de habilitarla.**

Configura la zona horaria del proceso Java coherentemente con la operación:

```powershell
$env:JAVA_TOOL_OPTIONS = "-Duser.timezone=America/Bogota"
```

Las fechas operativas actuales se serializan sin offset. No se ha migrado todo el
contrato a instantes UTC; verifica viajes/canjes cercanos a medianoche y clientes en
otras zonas. El PDF del voucher es la función de impresión del navegador, no un archivo
de servidor con firma o validez fiscal.

## 9. Publicación

1. Elige alojamiento y dominio. El agente no contrata ni paga servicios por ti.
2. Publica `frontend/dist/` tras `npm ci` y `npm run build`.
3. Ejecuta el JAR/backend con Java 21 y PostgreSQL en una red privada.
4. Usa un proxy HTTPS con frontend y `/api` en el mismo origen. Hay ejemplo en
   `frontend/deploy/nginx.conf`. No expongas la base de datos al público.
5. Configura `SESSION_COOKIE_SECURE=true` y no uses perfil local en producción.
6. Conserva Authorization, cookies y cabeceras CSRF; el webhook debe ser accesible
   por el proveedor, sin autenticación Basic añadida en el proxy.
7. Habilita copias de seguridad y prueba restaurarlas. Conserva logs con retención
   limitada, sin contraseñas, tokens, cuerpos de recuperación o datos de tarjetas.
8. Prueba HTTPS, cámara, cookies, expiración/logout, correos y sandbox en el dominio real.

Las sesiones viven en memoria del servidor: sobreviven recargas del navegador, no
reinicios del backend ni múltiples réplicas sin sesión compartida. Para escalar se
necesita almacenamiento de sesión compartido o afinidad revisada. Los límites de
intentos son locales por instancia/IP; tras un proxy configura correctamente las
IPs confiables y añade límites en el borde. No confíes en X-Forwarded-For arbitrario.

## 10. Qué sigue requiriendo desarrollo adicional

No se presenta este prototipo como un producto financiero listo para producción:

- Pagos productivos, split de marketplace/comisiones, reembolsos iniciados desde la
  aplicación y conciliación contable masiva no están habilitados.
- Galería/carga directa independiente del perfil de agencia, métricas de visitas
  con consentimiento, limpieza automática de cargas abandonadas y retención
  automática de tokens/auditorías requieren otra etapa.
- Las migraciones son SQL revisable, no una línea base automatizada de Flyway.
- Falta una auditoría formal de accesibilidad, seguridad y cumplimiento legal.

Esto **no es configuración que debas programar tú a ciegas**: son ampliaciones
explícitas del producto. Tus tareas inmediatas son configurar los proveedores,
los datos reales y comprobar las pruebas de aceptación de esta guía.

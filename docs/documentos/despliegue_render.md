# Rodatours en Render: base nueva

## Arquitectura y costes

Un servicio Docker en Oregon sirve React con nginx y `/api` con Java 21 en loopback.
Render termina HTTPS. Una sola instancia: las sesiones son en memoria y un reinicio
cierra sesiones. `render.yaml` utiliza exclusivamente el plan **Free**, por decisión
del propietario: no autoriza costes ni actualizaciones de plan. No crea otra PostgreSQL ni contrata nada
por sí solo. También puedes crear el Web Service manualmente con estos ajustes.

### Límites del despliegue gratuito

Según https://render.com/docs/free (consultado el 25/11/2026):

- Confirmar que la PostgreSQL existente también es **Free**. Caduca a los 30 días
  y no incluye backups: exportar y verificar respaldos manuales antes de caducar.
- El servicio duerme tras 15 minutos sin tráfico; despertar tarda alrededor de
  un minuto. Puede reiniciarse y perder sesiones. No es una publicación productiva
  con disponibilidad garantizada.
- No añadir método de pago para evitar cobros por excesos de uso. Sin método de
  pago, al superar ciertas cuotas Render suspende servicios o nuevos builds.
  Revisar Billing y cuotas; no aprobar upgrades ni servicios facturables.
- Free bloquea SMTP saliente en 25/465/587: el correo actual por SMTP 587 no
  funcionará. Mantener correo y recuperación deshabilitados hasta integrar y
   validar Mailgun por 2525 con STARTTLS obligatorio (puerto documentado por el
   proveedor). Si falla o no hay cuota gratuita vigente, evaluar una API HTTPS
   gratuita. Nunca simular entrega ni desactivar TLS para sortearlo.
- Mientras falte correo real, no declarar completada la validación de recuperación
  ni las notificaciones de correo. El webhook sandbox también debe probarse
  después de un despertar del servicio.

La imagen se verifica con `docker build -t rodatours:render-check .` y
`powershell.exe -NoProfile -File .\ops\test-render.ps1`. Esa prueba crea y elimina
sus propios contenedores PostgreSQL y web, sin conectarse a bases existentes.

Base elegida: la PostgreSQL existente en Oregon con identificador
`dpg-dc3g1b49v7es73bpggi0-a`. La base local no se modifica ni se copia.

## 1. Publicar el código y crear Web Service

Después de revisar los cambios, commit y push a `frontend` con autorización del
propietario. En Render: New → Web Service → repositorio → rama `frontend` →
Docker → Oregon. Dockerfile `./Dockerfile`, contexto raíz, una instancia.
Health check: `/api/experience-types` (comprueba también acceso al esquema).
Desactivar despliegue automático durante la inicialización.

## 2. Variables iniciales (sólo en Render)

| Variable | Configuración |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host-interno>:5432/<nombre-base>` desde los datos de conexión de Render. No pegar la URL `postgresql://usuario:contraseña@...` directamente: JDBC necesita este formato y credenciales separadas. |
| `DB_USERNAME`, `DB_PASSWORD` | Usuario y contraseña de esa base. |
| `SESSION_COOKIE_SECURE` | `true` |
| `SERVER_FORWARD_HEADERS_STRATEGY` | `framework` |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=65.0` |
| `APP_PUBLIC_URL` | URL HTTPS asignada al servicio, sin barra final. |
| `ADMIN_EMAIL` | Correo real del administrador. |
| `ADMIN_PASSWORD` | Contraseña nueva, 12 caracteres mínimo y 72 bytes máximo. |
| `VITE_MAPTILER_KEY` | Clave pública del navegador; restringir al dominio final. Se incorpora durante el build. |

No usar `SPRING_PROFILES_ACTIVE=local`. No subir `.env`, volcados ni secretos.
Los cambios de variables `VITE_*` requieren recompilar la imagen. No hay datos
de ejemplo: después de inicializar, el catálogo estará vacío.

## 3. Inicializar exclusivamente la base nueva y vacía

1. Confirmar que `DB_URL` corresponde a la nueva base, no a otra base con datos.
2. Añadir temporalmente `SPRING_JPA_HIBERNATE_DDL_AUTO=update` y desplegar.
3. Verificar arranque correcto, health check HTTP 200 y acceso del administrador.
4. **Eliminar** esa variable y volver a desplegar inmediatamente. El valor normal
   es `validate`: confirmar que el segundo arranque valida el esquema correctamente.
5. Eliminar `ADMIN_PASSWORD` y `ADMIN_EMAIL` después de crear el administrador;
   no son necesarios para iniciar sesión ni para los arranques posteriores.

No ejecutar los scripts aditivos de `docs/sql/` sobre este esquema ya generado.
No usar `create` ni `create-drop`. `update` sólo se permite aquí para la base vacía;
las futuras modificaciones necesitan SQL revisado y respaldo previo.

## 4. Integraciones y secretos

Configurar Cloudinary: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`,
`CLOUDINARY_API_SECRET`, `CLOUDINARY_VIDEO_PRESET`, `CLOUDINARY_IMAGE_PRESET`.

Correo: `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`,
`SMTP_AUTH=true`, `SMTP_STARTTLS=true`, `MAIL_ENABLED=true`,
`PASSWORD_RESET_ENABLED=true` y `RESET_ENCRYPTION_KEY` con el formato descrito en
la guía operativa. Conservar esa clave entre reinicios. Verificar entrega real.

En Render Free usar `SMTP_PORT=2525` para Mailgun y
`SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED=true`. El Blueprint deja
preparados puerto, autenticación y TLS, pero no activa envíos por defecto.
Confirmar cuota gratuita vigente y destinatarios autorizados si se usa sandbox.
La compatibilidad del puerto debe comprobarse desde Render: que Mailgun lo
admita no acredita la conectividad ni la entrega. No activar planes de pago,
pruebas que se renueven automáticamente con cargo ni excesos facturables.

Mercado Pago: `MP_ENABLED=true`, `MP_ACCESS_TOKEN`, `MP_WEBHOOK_SECRET`,
`MP_COLLECTOR_ID`, `MP_TEST_BUYER_ID`, y
`MP_NOTIFICATION_URL=https://<dominio>/api/payments/webhook`.
Mantener exclusivamente cuentas y pagos de prueba; no desactivar firmas ni
validaciones HTTPS. Rotar primero el token expuesto en el chat. En el panel del
proveedor configurar esa URL para **Pagos (legacy)** y su secreto correspondiente.

MapTiler Allowed HTTP origins: dominio definitivo sin protocolo ni puerto.
`VITE_MAP_TILE_URL` y `VITE_MAP_ATTRIBUTION` pueden quedar sin definir.

## 5. Aceptación y operación

- HTTPS, rutas React al recargar, archivos estáticos y catálogo vacío sin errores.
- Acceso, CSRF, cookies Secure/HttpOnly/SameSite, logout y permisos por rol.
- Carga manual de agencias, guías, tipos, tours y horarios reales.
- Mapa, imágenes y video; recuperación de contraseña con correo real.
- Reserva como turista, confirmación por operador, pago sandbox verificado por
  API y webhook, duplicados/firma inválida; voucher y QR; revisión móvil.
- Revisar logs sin secretos. Activar copias de seguridad según plan y probar
  restauración en una base separada antes de declarar la entrega completa.

Un build local correcto no acredita el despliegue: cerrar únicamente tras estas
comprobaciones en la URL pública. Para probar el contenedor localmente, usar una
PostgreSQL aislada y variables en un archivo privado fuera del repositorio;
la configuración nginx fuerza HTTPS porque está diseñada para el proxy de Render.

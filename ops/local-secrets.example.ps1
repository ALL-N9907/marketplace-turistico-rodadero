# Copia a ops/local-secrets.ps1 (ignorado por Git). Sólo para desarrollo local.
# No compartas este archivo ni lo incluyas en el frontend. Contiene secretos en texto plano.
# Completa los valores antes de descomentar. Las variables no definidas se conservan.
# $env:CLOUDINARY_CLOUD_NAME = ''
# $env:CLOUDINARY_API_KEY = ''
# $env:CLOUDINARY_API_SECRET = ''
# $env:CLOUDINARY_VIDEO_PRESET = 'rodatours_video'
# $env:CLOUDINARY_IMAGE_PRESET = 'rodatours_image'
# $env:SMTP_HOST = 'smtp.mailgun.org'
# $env:SMTP_PORT = '587'
# $env:SMTP_USERNAME = ''
# $env:SMTP_PASSWORD = ''
# $env:SMTP_FROM = ''
# $env:PASSWORD_RESET_ENABLED = 'true'
# $env:RESET_ENCRYPTION_KEY = '' # Reutiliza la clave existente, no generes otra al reiniciar.
# $env:APP_PUBLIC_URL = 'http://127.0.0.1:5173'
# Mercado Pago: copiar credenciales y firma del panel; no inventar la firma.
# $env:MP_ENABLED = 'true'
# $env:MP_ACCESS_TOKEN = ''
# $env:MP_COLLECTOR_ID = ''
# $env:MP_WEBHOOK_SECRET = ''
# $env:MP_NOTIFICATION_URL = 'https://<tunel-backend>/api/payments/webhook'
# APP_PUBLIC_URL también debe ser HTTPS para pagos.
# Opcional, sólo tras verificar en el panel que el ID pertenece a un comprador de prueba:
# $env:MP_TEST_BUYER_ID = ''

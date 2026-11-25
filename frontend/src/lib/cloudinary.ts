// No propagar cuerpos del proveedor: pueden contener firmas, parámetros o identificadores.
export function cloudinaryUploadError(status: number): Error {
  if (status === 401 || status === 403) return new Error('Cloudinary rechazó la autorización. Revisa API key, API secret y preset firmado en el backend; después elimina la carga pendiente y vuelve a intentarlo.');
  if (status === 429) return new Error('Cloudinary alcanzó su límite de solicitudes. Espera antes de reintentar.');
  if (status === 400) return new Error('Cloudinary rechazó el archivo o los parámetros. Revisa los límites del archivo y el preset firmado.');
  return new Error(`No se pudo cargar el archivo en Cloudinary (HTTP ${status}). No se publicó el contenido.`);
}

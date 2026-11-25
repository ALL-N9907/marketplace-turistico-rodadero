import { describe, expect, it } from 'vitest';
import { cloudinaryUploadError } from './cloudinary';

describe('diagnóstico de carga Cloudinary', () => {
  it('distingue autorización, límites y archivos inválidos sin usar cuerpos remotos', () => {
    expect(cloudinaryUploadError(401).message).toContain('autorización');
    expect(cloudinaryUploadError(403).message).toContain('preset firmado');
    expect(cloudinaryUploadError(429).message).toContain('límite');
    expect(cloudinaryUploadError(400).message).toContain('archivo');
    expect(cloudinaryUploadError(503).message).toContain('HTTP 503');
  });
});

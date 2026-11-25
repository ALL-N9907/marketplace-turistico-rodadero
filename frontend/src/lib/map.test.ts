import { describe, expect, it } from 'vitest';
import { mapConfig } from './map';

describe('configuración de mapa', () => {
  it('no configura solicitudes sin clave ni proveedor', () => {
    expect(mapConfig({})).toBeNull();
    expect(mapConfig({ VITE_MAPTILER_KEY: '  ' })).toBeNull();
  });
  it('usa MapTiler con atribución y clave codificada', () => {
    const config = mapConfig({ VITE_MAPTILER_KEY: ' public&key ' });
    expect(config?.url).toBe('https://api.maptiler.com/maps/streets-v4/256/{z}/{x}/{y}.png?key=public%26key');
    expect(config?.attribution).toContain('MapTiler');
    expect(config?.attribution).toContain('OpenStreetMap');
    expect(config?.maxZoom).toBe(19);
  });
  it('conserva el proveedor alternativo y exige su atribución', () => {
    expect(mapConfig({ VITE_MAP_TILE_URL: 'https://tiles.example/{z}/{x}/{y}.png' })).toBeNull();
    expect(mapConfig({ VITE_MAPTILER_KEY: 'public', VITE_MAP_TILE_URL: 'https://tiles.example/{z}/{x}/{y}.png', VITE_MAP_ATTRIBUTION: 'Proveedor' })?.attribution).toBe('Proveedor');
  });
});

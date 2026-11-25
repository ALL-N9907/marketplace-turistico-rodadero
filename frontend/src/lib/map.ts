export const MAPTILER_ATTRIBUTION = '&copy; <a href="https://www.maptiler.com/copyright/" target="_blank" rel="noopener noreferrer">MapTiler</a> &copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors';

export function mapConfig(env: { VITE_MAPTILER_KEY?: string; VITE_MAP_TILE_URL?: string; VITE_MAP_ATTRIBUTION?: string }) {
  const customUrl = env.VITE_MAP_TILE_URL?.trim();
  if (customUrl) {
    const attribution = env.VITE_MAP_ATTRIBUTION?.trim();
    return attribution ? { url: customUrl, attribution, maxZoom: 19 } : null;
  }
  const key = env.VITE_MAPTILER_KEY?.trim();
  if (!key) return null;
  return {
    url: `https://api.maptiler.com/maps/streets-v4/256/{z}/{x}/{y}.png?key=${encodeURIComponent(key)}`,
    attribution: MAPTILER_ATTRIBUTION,
    maxZoom: 19,
  };
}

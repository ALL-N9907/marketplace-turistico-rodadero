import { MapContainer, TileLayer, CircleMarker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import type { Agency, AgencyLocation } from '../types';
import { mapConfig } from '../lib/map';
export default function AgencyMap({ locations }: { locations: { agency: Agency; location: AgencyLocation }[] }) {
  const config=mapConfig({VITE_MAPTILER_KEY:import.meta.env.VITE_MAPTILER_KEY,VITE_MAP_TILE_URL:import.meta.env.VITE_MAP_TILE_URL,VITE_MAP_ATTRIBUTION:import.meta.env.VITE_MAP_ATTRIBUTION});
  if(!config)return <div className="map-placeholder"><span>◎</span><h3>El Rodadero, Santa Marta</h3><p>El mapa está listo para conectar MapTiler.</p><small>Configura VITE_MAPTILER_KEY con una clave pública restringida por dominio. El directorio funciona sin el mapa.</small></div>;
  const first=locations.find(l=>l.location.latitude!==null&&l.location.longitude!==null);
  return <MapContainer key={locations.map(l=>l.agency.id).join(',')} center={first?[first.location.latitude!,first.location.longitude!]:[11.205,-74.226]} zoom={14} maxZoom={config.maxZoom} scrollWheelZoom={false} className="agency-map"><TileLayer url={config.url} attribution={config.attribution} maxZoom={config.maxZoom}/>{locations.filter(l=>l.location.latitude!==null&&l.location.longitude!==null).map(({agency,location})=><CircleMarker key={agency.id} center={[location.latitude!,location.longitude!]} radius={9} pathOptions={{color:'#087f80',fillColor:'#087f80',fillOpacity:0.8}}><Popup><strong>{agency.nombreComercial}</strong><p>{agency.direccion}</p><a href={`/?agencyId=${agency.id}`}>Ver experiencias</a></Popup></CircleMarker>)}</MapContainer>;
}

import { lazy, Suspense, useState, type FormEvent } from 'react';
import { useQueries, useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ArrowRight, MapPin, Phone, Search } from 'lucide-react';
import { api, query } from '../lib/api';
import type { Agency, AgencyLocation } from '../types';
import { Empty, ErrorBox, Field, Heading, Loading, Notice } from '../components/ui';
const AgencyMap=lazy(()=>import('../components/AgencyMap'));
export function Agencies(){
  const [search,setSearch]=useState('');
  const [nearby,setNearby]=useState<{latitude:number;longitude:number;radiusKm:number}|null>(null);
  const [latitude,setLatitude]=useState(''),[longitude,setLongitude]=useState(''),[geoError,setGeoError]=useState('');
  const agencies=useQuery<Agency[]>({queryKey:['agencies-directory',search,nearby],queryFn:async({signal})=>nearby?(await api<{agency:Agency;distanceKm:number}[]>('/api/agencies/nearby',{method:'POST',body:JSON.stringify(nearby),signal})).map(item=>({...item.agency,distanceKm:item.distanceKm})):api<Agency[]>(`/api/agencies${query(search?{query:search}:{activeOnly:true})}`,{signal})});
  const locations=useQueries({queries:(agencies.data||[]).map(a=>({queryKey:['agency-location',a.id],queryFn:({signal}:{signal:AbortSignal})=>api<AgencyLocation>(`/api/agencies/${a.id}/location`,{signal})}))});
  const located=(agencies.data||[]).flatMap((agency,i)=>locations[i]?.data?[{agency,location:locations[i].data!}]:[]);
  function submit(e:FormEvent<HTMLFormElement>){e.preventDefault();setNearby(null);setSearch(String(new FormData(e.currentTarget).get('search')||''));}
  function locate(){setGeoError('');if(!navigator.geolocation){setGeoError('Tu navegador no dispone de geolocalización. Puedes introducir coordenadas.');return;}navigator.geolocation.getCurrentPosition(p=>{setLatitude(String(p.coords.latitude));setLongitude(String(p.coords.longitude));},()=>setGeoError('No se pudo obtener la ubicación. Revisa los permisos o introduce coordenadas.'),{timeout:10000,maximumAge:60000});}
  return <div className="container page-space">
    <Heading eyebrow="PERSONAS LOCALES. EXPERIENCIAS REALES." title="Conoce a tus próximos anfitriones">Busca agencias activas por nombre, dirección o cercanía.</Heading>
    <form className="inline-search" onSubmit={submit}><Field label="Nombre o dirección"><input name="search" placeholder="Busca una agencia en El Rodadero…" maxLength={255}/></Field><button className="button"><Search size={18}/> Buscar agencias</button></form>
    <SectionForNearby/>
    <form className="panel" onSubmit={e=>{e.preventDefault();setSearch('');setNearby({latitude:Number(latitude),longitude:Number(longitude),radiusKm:Number(new FormData(e.currentTarget).get('radius'))});}}>
      <h2>Agencias cerca de ti</h2><button className="button secondary" type="button" onClick={locate}>Usar mi ubicación</button>
      <div className="form-grid"><Field label="Latitud de búsqueda"><input type="number" min={-90} max={90} step="any" required value={latitude} onChange={e=>setLatitude(e.target.value)}/></Field><Field label="Longitud de búsqueda"><input type="number" min={-180} max={180} step="any" required value={longitude} onChange={e=>setLongitude(e.target.value)}/></Field><Field label="Radio · km"><input name="radius" type="number" min={0.1} max={200} step={0.1} defaultValue={10} required/></Field></div>
      {geoError&&<Notice warning>{geoError}</Notice>}<div className="form-actions"><button className="button">Buscar por cercanía</button><button className="button secondary" type="button" onClick={()=>{setNearby(null);setSearch('');}}>Ver todas</button></div>
    </form>
    <ErrorBox error={agencies.error} retry={()=>void agencies.refetch()}/>
    {agencies.isPending?<Loading/>:<div className="directory-grid"><div className="agency-list">{agencies.data?.map((a,i)=><article className="agency-card panel" key={a.id}>
      {locations[i]?.data?.photoUrl&&<img src={locations[i].data!.photoUrl!} alt={`Fotografía de ${a.nombreComercial}`} loading="lazy"/>}<span className="eyebrow">AGENCIA LOCAL</span><h2>{a.nombreComercial}</h2><p>{a.descripcion||'Descubre las experiencias de esta agencia.'}</p><span><MapPin size={16}/> {a.direccion}</span><span><Phone size={16}/> {a.telefonoContacto}</span>
      {a.distanceKm!==undefined&&<p>A {a.distanceKm.toLocaleString('es-CO',{maximumFractionDigits:1})} km en línea recta</p>}
      <ErrorBox error={locations[i]?.error}/><Link to={`/?agencyId=${a.id}`} className="button secondary">Ver experiencias <ArrowRight size={17}/></Link>
    </article>)}{agencies.data?.length===0&&<Empty title="No encontramos agencias con esa búsqueda"/>}</div><div className="map-sticky"><Suspense fallback={<Loading/>}><AgencyMap locations={located}/></Suspense><small>Ubicaciones declaradas por las agencias. La disponibilidad se consulta por experiencia.</small></div></div>}
  </div>;
}
function SectionForNearby(){return <Notice>La ubicación sólo se solicita al pulsar el botón y no se guarda en la base de datos. Se muestran hasta 50 agencias con coordenadas, ordenadas por distancia en línea recta, no por tiempo de viaje.</Notice>;}

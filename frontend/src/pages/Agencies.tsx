import { lazy, Suspense, useState, type FormEvent } from 'react';
import { useQueries, useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ArrowRight, MapPin, Phone, Search } from 'lucide-react';
import { api, query } from '../lib/api';
import type { Agency, AgencyLocation } from '../types';
import { Empty, ErrorBox, Field, Heading, Loading } from '../components/ui';
const AgencyMap=lazy(()=>import('../components/AgencyMap'));
export function Agencies(){
  const [search,setSearch]=useState('');
  const agencies=useQuery({queryKey:['agencies-directory',search],queryFn:({signal})=>api<Agency[]>(`/api/agencies${query(search?{query:search}:{activeOnly:true})}`,{signal})});
  const locations=useQueries({queries:(agencies.data||[]).map(a=>({queryKey:['agency-location',a.id],queryFn:({signal}:{signal:AbortSignal})=>api<AgencyLocation>(`/api/agencies/${a.id}/location`,{signal})}))});
  const located=(agencies.data||[]).flatMap((agency,i)=>locations[i]?.data?[{agency,location:locations[i].data!}]:[]);
  function submit(e:FormEvent<HTMLFormElement>){e.preventDefault();setSearch(String(new FormData(e.currentTarget).get('search')||''));}
  return <div className="container page-space"><Heading eyebrow="PERSONAS LOCALES. EXPERIENCIAS REALES." title="Conoce a tus próximos anfitriones">Encuentra agencias activas por nombre o dirección. Sin búsqueda por distancia.</Heading><form className="inline-search" onSubmit={submit}><Field label="Nombre o dirección"><input name="search" placeholder="Busca una agencia en El Rodadero…" maxLength={255}/></Field><button className="button"><Search size={18}/> Buscar agencias</button></form><ErrorBox error={agencies.error} retry={()=>void agencies.refetch()}/>{agencies.isPending?<Loading/>:<div className="directory-grid"><div className="agency-list">{agencies.data?.map((a,i)=><article className="agency-card panel" key={a.id}>{locations[i]?.data?.photoUrl&&<img src={locations[i].data!.photoUrl!} alt={`Fotografía de ${a.nombreComercial}`} loading="lazy"/>}<span className="eyebrow">AGENCIA LOCAL</span><h2>{a.nombreComercial}</h2><p>{a.descripcion||'Descubre las experiencias de esta agencia.'}</p><span><MapPin size={16}/> {a.direccion}</span><span><Phone size={16}/> {a.telefonoContacto}</span><ErrorBox error={locations[i]?.error}/><Link to={`/?agencyId=${a.id}`} className="button secondary">Ver experiencias <ArrowRight size={17}/></Link></article>)}{agencies.data?.length===0&&<Empty title="No encontramos agencias con esa búsqueda"/>}</div><div className="map-sticky"><Suspense fallback={<Loading/>}><AgencyMap locations={located}/></Suspense><small>Ubicaciones declaradas por las agencias. La disponibilidad se consulta por experiencia.</small></div></div>}</div>;
}

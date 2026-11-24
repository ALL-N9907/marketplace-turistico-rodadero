import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../auth';
import { api, optional, write } from '../lib/api';
import type { Tour, TourDetails, UploadAuthorization, Video } from '../types';
import { EntityManager, type FormField } from '../components/EntityManager';
import { ErrorBox, Field, Heading, Loading, Notice, Section } from '../components/ui';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { TourGalleryEditor } from '../components/TourGallery';

export function TourContent(){
  const {id}=useParams();return <><TourContentBody/><TourGalleryEditor tourId={id!}/></>;
}
function TourContentBody(){
  const {id}=useParams(),client=useQueryClient(),[saved,setSaved]=useState(false);
  const tour=useQuery({queryKey:['tour',id],queryFn:({signal})=>api<Tour>(`/api/tours/${id}`,{signal})});
  const details=useQuery({queryKey:['tour-details',id],queryFn:({signal})=>api<TourDetails>(`/api/tours/${id}/details`,{signal})});
  const mutation=useMutation({mutationFn:(body:unknown)=>write(`/api/tours/${id}/details`,'PUT',body),onSuccess:()=>{setSaved(true);void client.invalidateQueries({queryKey:['tour-details',id]});void client.invalidateQueries({queryKey:['catalog']});}});
  return <><Link className="back-link" to="/panel/tours">← Volver al catálogo</Link><Heading title={tour.data?.titulo||'Detalles del tour'}>Los detalles y el video se guardan por separado del precio y los datos principales.</Heading><Section title="Detalles de la experiencia"><ErrorBox error={details.error}/>{details.isPending?<Loading/>:details.data&&<form key={details.dataUpdatedAt} onSubmit={e=>{e.preventDefault();setSaved(false);const f=new FormData(e.currentTarget);mutation.mutate({itinerary:f.get('itinerary')||null,conditions:f.get('conditions')||null,supportsCommunities:f.has('supportsCommunities'),environmentalPractices:f.has('environmentalPractices')});}}><Field label="Itinerario"><textarea name="itinerary" defaultValue={details.data.itinerary||''} maxLength={10000} rows={5}/></Field><Field label="Condiciones"><textarea name="conditions" defaultValue={details.data.conditions||''} maxLength={10000} rows={4}/></Field><label className="check"><input name="supportsCommunities" type="checkbox" defaultChecked={details.data.supportsCommunities}/> Apoyo a comunidades locales</label><label className="check"><input name="environmentalPractices" type="checkbox" defaultChecked={details.data.environmentalPractices}/> Prácticas ambientales</label><Notice>Estas prácticas son declaraciones del operador, no certificaciones verificadas.</Notice><ErrorBox error={mutation.error}/>{saved&&<Notice>Detalles guardados.</Notice>}<button className="button" disabled={mutation.isPending}>Guardar detalles</button></form>}</Section><VideoManager id={id!}/></>;
}
function VideoManager({id}:{id:string}){
  const client=useQueryClient(),[stage,setStage]=useState(''),[deleting,setDeleting]=useState(false),[file,setFile]=useState<File|null>(null);
  const video=useQuery({queryKey:['video',id],queryFn:({signal})=>optional(api<Video>(`/api/tours/${id}/video`,{signal}))});
  const upload=useMutation({mutationFn:async()=>{
    if(!file)throw new Error('Selecciona un video.');
    if(!/\.(mp4|webm|mov)$/i.test(file.name)||file.size>50*1024*1024)throw new Error('Elige MP4, WebM o MOV de máximo 50 MiB.');
    setStage('Solicitando autorización firmada…');const authorization=await write<UploadAuthorization>(`/api/tours/${id}/video/upload-authorization`,'POST');
    if(file.size>authorization.maxBytes)throw new Error('El archivo supera el límite autorizado por el servidor.');
    const url=new URL(authorization.upload.uploadUrl);
    if(url.protocol!=='https:'||url.hostname!=='api.cloudinary.com')throw new Error('El servidor devolvió un proveedor de carga no permitido.');
    const form=new FormData();Object.entries(authorization.upload.parameters).forEach(([k,v])=>form.append(k,v));form.append('file',file);form.append('api_key',authorization.upload.apiKey);form.append('signature',authorization.upload.signature);
    setStage('Cargando a Cloudinary. Espera a que termine…');const result=await fetch(url,{method:'POST',body:form,credentials:'omit'});
    if(!result.ok)throw new Error('Cloudinary rechazó la carga. El video no ha sido publicado.');
    setStage('Verificando el archivo con el servidor…');await write(`/api/tours/${id}/video/complete`,'POST');
  },onSuccess:()=>{setStage('Video verificado y publicado.');setFile(null);void client.invalidateQueries({queryKey:['video',id]});},onError:()=>setStage('La carga no se publicó. Elimina la sesión pendiente antes de iniciar otra; puedes reintentar la verificación si el archivo ya se cargó.')});
  const remove=useMutation({mutationFn:()=>write(`/api/tours/${id}/video`,'DELETE'),onSuccess:()=>{setDeleting(false);setStage('Video o carga pendiente eliminados.');upload.reset();void client.invalidateQueries({queryKey:['video',id]});}});
  const verify=useMutation({mutationFn:()=>write(`/api/tours/${id}/video/complete`,'POST'),onSuccess:()=>{setStage('Video verificado y publicado.');void client.invalidateQueries({queryKey:['video',id]});}});
  return <Section title="Video de la experiencia"><ErrorBox error={video.error}/>{video.data&&<video className="tour-video" controls src={video.data.url} preload="metadata"/>}<p>Un video por tour · MP4, WebM o MOV · 50 MiB y 120 segundos como máximo.</p><Field label="Seleccionar archivo de video"><input type="file" accept=".mp4,.webm,.mov,video/mp4,video/webm,video/quicktime" disabled={upload.isPending||!!video.data} onChange={e=>{setFile(e.target.files?.[0]||null);upload.reset();}}/></Field><div className="form-actions"><button className="button" disabled={!file||upload.isPending||verify.isPending||!!video.data} onClick={()=>upload.mutate()}>Cargar y verificar video</button><button className="button secondary" disabled={upload.isPending||verify.isPending} onClick={()=>verify.mutate()}>Reintentar verificación</button><button className="text-button danger-text" disabled={upload.isPending||verify.isPending} onClick={()=>{setDeleting(true);remove.reset();}}>Eliminar video o carga pendiente</button></div>{stage&&<Notice>{stage}</Notice>}<ErrorBox error={upload.error||verify.error}/><small>No se publica el video hasta que el servidor lo verifique. La eliminación en el CDN puede tardar en propagarse.</small>{deleting&&<ConfirmDialog title="Eliminar video o sesión de carga" busy={remove.isPending} onCancel={()=>setDeleting(false)} onSubmit={e=>{e.preventDefault();remove.mutate();}}><p>Se solicitará la eliminación con invalidación de caché. Después podrás cargar un video nuevo.</p><ErrorBox error={remove.error}/></ConfirmDialog>}</Section>;
}
export function Schedules(){
  const {id}=useParams(),{user}=useAuth();
  const tour=useQuery({queryKey:['tour',id],queryFn:({signal})=>api<Tour>(`/api/tours/${id}`,{signal})});
  const fields:FormField[]=[{key:'fechaTour',label:'Fecha de salida',type:'date',required:true},{key:'horaInicio',label:'Hora de salida',type:'time',required:true,step:1},{key:'cuposTotales',label:'Cupos totales',type:'number',min:1,required:true},{key:'guiaId',label:'ID del guía · opcional',type:'number',min:1}];
  return <><Link className="back-link" to="/panel/tours">← Volver al catálogo</Link><Notice>La disponibilidad pública considera solicitudes pendientes. Los cupos de esta tabla son el stock del horario. Usa un guía activo de la misma agencia; el servidor valida cruces y duplicados.</Notice><EntityManager title={`Salidas · ${tour.data?.titulo||'Tour #'+id}`} description={`Gestiona la agenda y capacidad. ${user?.agencyId?'Sólo salidas de tu agencia.':''}`} listPath={`/api/tours/${id}/schedules`} writePath={`/api/tours/${id}/schedules`} updatePath="/api/schedules" fields={fields} canDelete columns={[{key:'id',label:'ID de salida'},{key:'fechaTour',label:'Fecha'},{key:'horaInicio',label:'Hora'},{key:'cuposTotales',label:'Cupos totales'},{key:'cuposDisponibles',label:'Stock del horario'},{key:'guiaId',label:'Guía'}]} actions={r=><Link to={`/panel/reservas?horarioId=${r.id}`}>Ver reservas →</Link>}/></>;
}

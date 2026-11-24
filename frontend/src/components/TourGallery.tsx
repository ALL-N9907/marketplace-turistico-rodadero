import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, write } from '../lib/api';
import { ConfirmDialog } from './ConfirmDialog';
import { Empty, ErrorBox, Field, Notice, Section } from './ui';

interface Image {id:number;status:string;url:string|null;expiresAt:string}
interface Upload {id:number;maxBytes:number;upload:{uploadUrl:string;apiKey:string;signature:string;parameters:Record<string,string>}}
export function TourGallery({tourId}:{tourId:number}){
  const result=useQuery({queryKey:['tour-gallery',tourId],queryFn:({signal})=>api<Image[]>(`/api/tours/${tourId}/images`,{signal})});
  const [selected,setSelected]=useState<number|null>(null);
  if(result.error)return <ErrorBox error={result.error} retry={()=>void result.refetch()}/>;
  const images=result.data?.filter(i=>i.url)||[];
  if(!images.length)return null;
  const current=images.find(i=>i.id===selected)||images[0];
  return <Section title="Así se vive esta experiencia"><img className="gallery-feature" src={current.url!} alt="Fotografía de la experiencia publicada por la agencia"/><div className="gallery-thumbs">{images.map((image,index)=><button key={image.id} type="button" aria-label={`Ver fotografía ${index+1}`} aria-pressed={current.id===image.id} onClick={()=>setSelected(image.id)}><img src={image.url!} alt="" loading="lazy"/></button>)}</div></Section>;
}
export function TourGalleryEditor({tourId}:{tourId:string}){
  const client=useQueryClient(),[file,setFile]=useState<File|null>(null),[deleting,setDeleting]=useState<number|null>(null),[stage,setStage]=useState('');
  const result=useQuery({queryKey:['tour-gallery-manage',tourId],queryFn:({signal})=>api<Image[]>(`/api/tours/${tourId}/images/uploads`,{signal})});
  function refresh(){void client.invalidateQueries({queryKey:['tour-gallery']});void client.invalidateQueries({queryKey:['tour-gallery-manage']});}
  const upload=useMutation({mutationFn:async()=>{
    if(!file||file.size>5*1024*1024||!/\.(jpe?g|png|webp)$/i.test(file.name))throw new Error('Selecciona JPEG, PNG o WebP de máximo 5 MiB.');
    setStage('Autorizando imagen…');const auth=await write<Upload>(`/api/tours/${tourId}/images`,'POST');refresh();
    const url=new URL(auth.upload.uploadUrl);if(url.protocol!=='https:'||url.hostname!=='api.cloudinary.com'||!url.pathname.endsWith('/image/upload'))throw new Error('Proveedor de imagen no permitido.');
    if(file.size>auth.maxBytes)throw new Error('El archivo supera el límite del servidor.');
    const body=new FormData();Object.entries(auth.upload.parameters).forEach(([k,v])=>body.append(k,v));body.append('file',file);body.append('api_key',auth.upload.apiKey);body.append('signature',auth.upload.signature);
    setStage('Cargando a Cloudinary…');const response=await fetch(url,{method:'POST',credentials:'omit',body});if(!response.ok)throw new Error('La carga fue rechazada. No se publicó la fotografía.');
    setStage('Verificando la imagen…');await write(`/api/tours/${tourId}/images/${auth.id}/complete`,'POST');
  },onSuccess:()=>{setStage('Fotografía verificada y publicada.');setFile(null);refresh();},onError:()=>{setStage('La imagen no se publicó. Puedes verificar o eliminar la carga pendiente.');refresh();}});
  const verify=useMutation({mutationFn:(id:number)=>write(`/api/tours/${tourId}/images/${id}/complete`,'POST'),onSuccess:refresh});
  const remove=useMutation({mutationFn:()=>write(`/api/tours/${tourId}/images/${deleting}`,'DELETE'),onSuccess:()=>{setDeleting(null);refresh();}});
  return <Section title="Galería de fotografías"><Notice>Máximo 6 imágenes, incluidas las cargas pendientes. JPEG, PNG o WebP; 5 MiB por archivo. No subas datos personales. Las imágenes son públicas en Cloudinary.</Notice>
    <Field label="Fotografía"><input type="file" accept=".jpg,.jpeg,.png,.webp" disabled={upload.isPending} onChange={e=>{setFile(e.target.files?.[0]||null);upload.reset();}}/></Field>
    <button className="button" disabled={!file||upload.isPending} onClick={()=>upload.mutate()}>Cargar y publicar fotografía</button>
    {stage&&<Notice>{stage}</Notice>}<ErrorBox error={upload.error||verify.error||result.error}/>
    <div className="gallery-grid">{result.data?.map(image=><article key={image.id} className="panel">{image.url?<img src={image.url} alt="Fotografía publicada"/>:<Empty title="Carga pendiente"/>}<p>#{image.id} · {image.status}</p><div className="form-actions">{image.status!=='READY'&&<button className="button secondary" disabled={verify.isPending||upload.isPending} onClick={()=>verify.mutate(image.id)}>Verificar</button>}<button className="text-button danger-text" disabled={upload.isPending} onClick={()=>{setDeleting(image.id);remove.reset();}}>Eliminar</button></div></article>)}</div>
    {deleting!==null&&<ConfirmDialog title="Eliminar fotografía o carga pendiente" busy={remove.isPending} onCancel={()=>setDeleting(null)} onSubmit={e=>{e.preventDefault();remove.mutate();}}><p>La eliminación es definitiva y solicita invalidación del CDN. La propagación puede tardar.</p><ErrorBox error={remove.error}/></ConfirmDialog>}
  </Section>;
}

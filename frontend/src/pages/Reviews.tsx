import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { api, write } from '../lib/api';
import type { Review } from '../types';
import { Empty, ErrorBox, Field, Heading, Loading, Notice, Section } from '../components/ui';
import { ConfirmDialog } from '../components/ConfirmDialog';
export function Reviews(){
  const [params]=useSearchParams(),client=useQueryClient();const [editing,setEditing]=useState<Review|null>(null),[deleting,setDeleting]=useState<Review|null>(null),[success,setSuccess]=useState(false);
  const result=useQuery({queryKey:['reviews'],queryFn:({signal})=>api<Review[]>('/api/reviews',{signal})});
  const save=useMutation({mutationFn:(body:unknown)=>write(editing?`/api/reviews/${editing.id}`:'/api/reviews',editing?'PUT':'POST',body),onSuccess:()=>{setEditing(null);setSuccess(true);void client.invalidateQueries({queryKey:['reviews']});void client.invalidateQueries({queryKey:['public-reviews']});}});
  const remove=useMutation({mutationFn:()=>write(`/api/reviews/${deleting!.id}`,'DELETE'),onSuccess:()=>{setDeleting(null);void client.invalidateQueries({queryKey:['reviews']});void client.invalidateQueries({queryKey:['public-reviews']});}});
  return <div className="container page-space narrow">
    <Heading eyebrow="HISTORIAS QUE AYUDAN A ELEGIR" title="Tus reseñas">Comparte tu experiencia después de completar una reserva.</Heading>
    <Notice>Tu calificación y comentario serán públicos en la página del tour. No incluyas teléfonos, correos, documentos ni información personal de terceros.</Notice>
    {success&&<Notice>Reseña guardada correctamente.</Notice>}
    <Section title={editing?'Edita tu reseña':'Cuéntanos cómo te fue'}>
      <form key={editing?.id||'new'} onSubmit={e=>{
        e.preventDefault();setSuccess(false);const f=new FormData(e.currentTarget);
        save.mutate({...(!editing?{reservaId:Number(f.get('reservaId'))}:{}),calificacion:Number(f.get('calificacion')),comentario:f.get('comentario')||null});
      }}>
        {!editing&&<Field label="ID de la reserva completada"><input name="reservaId" type="number" min={1} step={1} defaultValue={params.get('reservaId')||''} required/></Field>}
        <Field label="Tu calificación"><select name="calificacion" defaultValue={editing?.calificacion||5}>{[5,4,3,2,1].map(n=><option key={n} value={n}>{'★'.repeat(n)} · {n} de 5</option>)}</select></Field>
        <Field label="Comentario público · opcional"><textarea name="comentario" defaultValue={editing?.comentario||''} maxLength={4000}/></Field>
        <ErrorBox error={save.error}/>
        <div className="form-actions">
          <button className="button" disabled={save.isPending}>Guardar reseña</button>
          {editing&&<button className="button secondary" type="button" onClick={()=>{setEditing(null);save.reset();}}>Volver a nueva reseña</button>}
        </div>
      </form>
    </Section>
    <h2>Lo que has compartido</h2>
    <ErrorBox error={result.error}/>
    {result.isPending?<Loading/>:result.data?.length?result.data.map(r=><article className="panel" key={r.id}>
      <strong className="review-stars" aria-label={`${r.calificacion} de 5 estrellas`}>{'★'.repeat(r.calificacion)}</strong>
      <h3>Reserva RT-{r.reservaId}</h3><p className="preserve-lines">{r.comentario}</p>
      <div className="form-actions">
        <button className="button secondary" disabled={save.isPending} onClick={()=>{setEditing(r);save.reset();setSuccess(false);window.scrollTo({top:0,behavior:'smooth'});}}>Editar</button>
        <button className="text-button danger-text" disabled={save.isPending} onClick={()=>{setDeleting(r);remove.reset();}}>Eliminar</button>
      </div>
    </article>):!result.error&&<Empty title="Aún no has compartido una reseña"/>}
    {deleting&&<ConfirmDialog title="Eliminar reseña" busy={remove.isPending} onCancel={()=>setDeleting(null)} onSubmit={e=>{e.preventDefault();remove.mutate();}}><p>Esta acción eliminará tu reseña definitivamente.</p><ErrorBox error={remove.error}/></ConfirmDialog>}
  </div>;
}

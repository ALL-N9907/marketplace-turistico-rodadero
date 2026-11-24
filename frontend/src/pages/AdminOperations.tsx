import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, write } from '../lib/api';
import { dateTime } from '../lib/format';
import { Empty, ErrorBox, Field, Heading, Loading, Notice, Section } from '../components/ui';

type Item={id:number;reservationId?:number;recipient?:string;attempts?:number;sentAt?:string;nextAttemptAt?:string;status?:string;providerPaymentId?:string;outcome?:string;rating?:number;comment?:string;published?:boolean;reason?:string};
export function AdminOperations(){
  const [area,setArea]=useState('mail'),[page,setPage]=useState(0),[selected,setSelected]=useState<Item|null>(null),client=useQueryClient();
  const path=area==='reviews'?'/api/admin/reviews':`/api/admin/deliveries/${area}`;
  const result=useQuery({queryKey:['admin-operations',area,page],queryFn:({signal})=>api<Item[]>(`${path}?page=${page}&size=20`,{signal})});
  const change=useMutation({mutationFn:(body?:unknown)=>area==='reviews'?write(`/api/admin/reviews/${selected!.id}/moderation`,'PUT',body):write(`${path}/${selected!.id}/retry`,'POST'),onSuccess:()=>{setSelected(null);void client.invalidateQueries({queryKey:['admin-operations']});void client.invalidateQueries({queryKey:['public-reviews']});}});
  return <><Heading title="Entregas y moderación">Operaciones exclusivas del administrador. No se muestran secretos ni enlaces de recuperación.</Heading>
    <div className="tabs">{[['mail','Correo de reservas'],['webhooks','Eventos de pago'],['reviews','Reseñas']].map(([value,label])=><button key={value} className={area===value?'active':''} onClick={()=>{setArea(value);setPage(0);setSelected(null);change.reset();}}>{label}</button>)}</div>
    <ErrorBox error={result.error}/>{result.isPending?<Loading/>:result.data?.length?<div className="table-wrap"><table><thead><tr><th>ID</th><th>Detalle</th><th>Estado</th><th>Acción</th></tr></thead><tbody>{result.data.map(item=><tr key={item.id}><td>{item.id}</td><td>{area==='mail'?`${item.recipient} · Reserva ${item.reservationId}`:area==='webhooks'?`Pago ${item.providerPaymentId} · ${item.outcome||'Pendiente'}`:item.comment||'Sin comentario'}</td><td>{area==='reviews'?(item.published?'Publicada':'Oculta'):area==='mail'?(item.sentAt?`Enviado ${dateTime(item.sentAt)}`:`Pendiente · ${item.attempts} intentos`):`${item.status} · ${item.attempts} intentos`}</td><td><button className="text-button" disabled={area==='mail'?!!item.sentAt:area==='webhooks'?item.status!=='FAILED':false} onClick={()=>{setSelected(item);change.reset();}}>{area==='reviews'?'Moderar':'Reintentar'}</button></td></tr>)}</tbody></table></div>:!result.error&&<Empty/>}
    <div className="pagination"><button className="button secondary" disabled={!page} onClick={()=>setPage(page-1)}>Anterior</button><span>Página {page+1}</span><button className="button secondary" disabled={!result.data||result.data.length<20} onClick={()=>setPage(page+1)}>Siguiente</button></div>
    {selected&&<Section title={`Registro #${selected.id}`}><form onSubmit={e=>{e.preventDefault();const f=new FormData(e.currentTarget);change.mutate(area==='reviews'?{published:f.get('published')==='true',reason:String(f.get('reason')).trim()}:undefined);}}>
      {area==='reviews'?<><Field label="Visibilidad"><select name="published" defaultValue={String(selected.published)}><option value="true">Publicada</option><option value="false">Oculta</option></select></Field><Field label="Motivo de moderación obligatorio"><textarea name="reason" required maxLength={1000} defaultValue={selected.reason||''}/></Field></>:<Notice>Se repondrá en la cola. El envío/procesamiento depende de que el servicio correspondiente esté configurado. No implica entrega inmediata.</Notice>}
      <ErrorBox error={change.error}/><div className="form-actions"><button className="button" disabled={change.isPending}>Confirmar operación</button><button className="button secondary" type="button" onClick={()=>setSelected(null)}>Cancelar</button></div>
    </form></Section>}
  </>;
}

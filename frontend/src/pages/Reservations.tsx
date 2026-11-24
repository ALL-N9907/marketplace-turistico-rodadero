import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useLocation, useParams } from 'react-router-dom';
import { Download, QrCode } from 'lucide-react';
import { useAuth } from '../auth';
import { api, apiBlob, query, write } from '../lib/api';
import { dateTime, money, reservationActions } from '../lib/format';
import type { Reservation, Voucher } from '../types';
import { Badge, Empty, ErrorBox, Field, Heading, Loading, Notice, Section } from '../components/ui';
import { ConfirmDialog } from '../components/ConfirmDialog';

export function Reservations(){
  const {user}=useAuth();const [status,setStatus]=useState('');
  const result=useQuery({queryKey:['my-reservations',user?.email,status],queryFn:({signal})=>api<Reservation[]>(`/api/reservations${query({turistaId:user!.touristId,estado:status})}`,{signal})});
  return <div className="container page-space"><Heading eyebrow="TU CUADERNO DE VIAJE" title="Tus próximas historias">Solicitudes, experiencias confirmadas y recuerdos por compartir.</Heading><div className="tabs">{[['','Todas'],['SCHEDULED','Pendientes'],['CONFIRMED','Confirmadas'],['COMPLETED','Completadas'],['CANCELLED','Canceladas'],['NO_SHOW','No asistió']].map(([v,label])=><button className={status===v?'active':''} key={v} onClick={()=>setStatus(v)}>{label}</button>)}</div><ErrorBox error={result.error} retry={()=>void result.refetch()}/>{result.isPending?<Loading/>:result.data?.length?<ReservationTable reservations={result.data}/>:!result.error&&<Empty title="Tu primera historia está por empezar"><Link to="/">Explora experiencias y elige tu próximo destino.</Link></Empty>}</div>;
}
export function ReservationTable({reservations,workspace=false}:{reservations:Reservation[];workspace?:boolean}){
  return <div className="table-wrap"><table><thead><tr><th>Reserva</th><th>Salida</th><th>Personas</th><th>Total</th><th>Estado</th><th>Detalle</th></tr></thead><tbody>{reservations.map(r=><tr key={r.id}><td><strong>RT-{r.id}</strong><small>Salida #{r.horarioId}</small></td><td>{dateTime(r.fechaInicio)}</td><td>{r.cantidadPersonas}</td><td>{money(r.precioTotal)}</td><td><Badge status={r.estado}/></td><td><Link className="text-link" to={`${workspace?'/panel':''}/reservas/${r.id}`}>Ver reserva →</Link></td></tr>)}</tbody></table></div>;
}
function PrivateQr({id}:{id:number}){
  const [url,setUrl]=useState(''),[error,setError]=useState<unknown>(null);
  useEffect(()=>{const abort=new AbortController();let objectUrl='';apiBlob(`/api/reservations/${id}/voucher/qr`,abort.signal).then(blob=>{if(abort.signal.aborted)return;objectUrl=URL.createObjectURL(blob);setUrl(objectUrl);}).catch(e=>{if(!abort.signal.aborted)setError(e);});return()=>{abort.abort();if(objectUrl)URL.revokeObjectURL(objectUrl);};},[id]);
  return <div className="private-qr"><ErrorBox error={error}/>{url?<><img src={url} alt={`Código QR privado de la reserva RT-${id}`}/><a className="button secondary" href={url} download={`reserva-${id}-qr.png`}><Download size={18}/> Descargar QR</a></>:!error&&<Loading/>}</div>;
}
export function ReservationDetail(){
  const {id}=useParams(),{user}=useAuth(),client=useQueryClient(),location=useLocation();
  const [action,setAction]=useState('');
  const result=useQuery({queryKey:['reservation',user?.email,id],queryFn:({signal})=>api<Reservation>(`/api/reservations/${id}`,{signal})});
  const voucher=useQuery({queryKey:['voucher',user?.email,id],queryFn:({signal})=>api<Voucher>(`/api/reservations/${id}/voucher`,{signal}),enabled:!!result.data});
  const update=useMutation({mutationFn:(body:unknown)=>write<Reservation>(`/api/reservations/${id}/${action}`,'PUT',body),onSuccess:async()=>{setAction('');await Promise.all([client.invalidateQueries({queryKey:['reservation']}),client.invalidateQueries({queryKey:['voucher']}),client.invalidateQueries({queryKey:['my-reservations']}),client.invalidateQueries({queryKey:['operation-reservations']}),client.invalidateQueries({queryKey:['availability']})]);},onError:()=>{void client.invalidateQueries({queryKey:['reservation']});}});
  if(result.isPending)return <Loading/>;
  if(!result.data||!user)return <ErrorBox error={result.error}/>;
  const r=result.data,v=voucher.data,actions=reservationActions(user,r),workspace=location.pathname.startsWith('/panel');
  return <div className={workspace?'':'container page-space'}>
    <Link className="back-link" to={workspace?'/panel/reservas':'/reservas'}>← Volver a reservas</Link>
    <Heading eyebrow={`RESERVA RT-${r.id}`} title={v?.tour||'Detalle de tu reserva'}>{v?.agency||'Comprobante privado de reserva. No es un recibo de pago.'}</Heading>
    {location.state?.created&&<Notice>Solicitud creada. La agencia debe confirmarla antes de que puedas realizar el check-in. No se ha realizado ningún cobro.</Notice>}
    <div className="reservation-detail">
      <Section title="Tu experiencia">
        <Badge status={r.estado}/>
        <dl className="data-list">
          <div><dt>Inicio</dt><dd>{dateTime(r.fechaInicio)}</dd></div>
          <div><dt>Fin</dt><dd>{dateTime(r.fechaFin)}</dd></div>
          <div><dt>Personas</dt><dd>{r.cantidadPersonas}</dd></div>
          <div><dt>Total de la reserva</dt><dd>{money(r.precioTotal)}</dd></div>
          <div><dt>Observaciones</dt><dd>{r.observaciones||'Sin observaciones'}</dd></div>
          {r.motivoCancelacion&&<div><dt>Motivo de cancelación</dt><dd>{r.motivoCancelacion}</dd></div>}
          <div><dt>Última modificación</dt><dd>{dateTime(r.fechaModificacion)}</dd></div>
          {v?.checkedInAt&&<div><dt>Check-in registrado</dt><dd>{dateTime(v.checkedInAt)}</dd></div>}
        </dl>
        <div className="form-actions">
          {actions.confirm&&<button className="button" onClick={()=>{update.reset();setAction('confirm');}}>Confirmar reserva</button>}
          {actions.cancel&&<button className="button secondary" onClick={()=>{update.reset();setAction('cancel');}}>Cancelar con motivo</button>}
          {actions.complete&&<button className="button" onClick={()=>{update.reset();setAction('complete');}}>Marcar completada</button>}
          {actions.noShow&&<button className="button secondary" onClick={()=>{update.reset();setAction('no-show');}}>Marcar no asistió</button>}
          {actions.review&&<Link className="button" to={`/resenas?reservaId=${r.id}`}>Escribir reseña</Link>}
          {actions.checkIn&&<Link className="button secondary" to={`/panel/validar?reservaId=${r.id}`}><QrCode size={18}/> Validar acceso</Link>}
        </div>
      </Section>
      <Section title="Voucher digital"><ErrorBox error={voucher.error}/>{r.estado==='CONFIRMED'?<><PrivateQr id={r.id}/><Notice>Canje único el día de la salida. El check-in no completa automáticamente la reserva.</Notice></>:<Notice>Estado: <Badge status={r.estado}/>. El QR para acceso se muestra cuando la reserva está confirmada.</Notice>}</Section>
    </div>
    {action&&<ConfirmDialog title={{confirm:'Confirmar reserva',cancel:'Cancelar reserva',complete:'Completar experiencia','no-show':'Registrar inasistencia'}[action]||''} busy={update.isPending} onCancel={()=>setAction('')} onSubmit={e=>{
      e.preventDefault();const f=new FormData(e.currentTarget);
      if(action==='cancel'&&!String(f.get('motivo')||'').trim())return;
      update.mutate(action==='cancel'?{motivo:String(f.get('motivo')).trim()}:action==='complete'?{observaciones:f.get('observaciones')||null}:undefined);
    }}>
      <p>{action==='cancel'?'Esta cancelación es definitiva. Escribe el motivo para continuar.':'El servidor verificará el estado, los permisos y la fecha antes de aplicar el cambio.'}</p>
      {action==='cancel'&&<Field label="Motivo obligatorio"><textarea name="motivo" required maxLength={2000} onChange={e=>e.target.setCustomValidity(e.target.value.trim()?'':'Escribe un motivo no vacío.')} autoFocus/></Field>}
      {action==='complete'&&<Field label="Observaciones administrativas · opcional"><textarea name="observaciones" maxLength={2000}/></Field>}
      <ErrorBox error={update.error}/>
    </ConfirmDialog>}
  </div>;
}

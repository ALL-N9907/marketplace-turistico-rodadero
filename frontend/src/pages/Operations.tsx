import { useEffect, useId, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Camera, CheckCircle2, QrCode } from 'lucide-react';
import { useAuth } from '../auth';
import { api, query, write } from '../lib/api';
import type { Reservation, Voucher } from '../types';
import { dateTime } from '../lib/format';
import { Empty, ErrorBox, Field, Heading, Loading, Notice, Section } from '../components/ui';
import { ReservationTable } from './Reservations';

export function OperationReservations(){
  const {user}=useAuth(),[params,setParams]=useSearchParams(),navigate=useNavigate(),client=useQueryClient();
  const schedule=params.get('horarioId')||'';
  const canList=user?.role==='ADMIN'||(user?.role==='AGENCY_OPERATOR'&&!!schedule);
  const result=useQuery({queryKey:['operation-reservations',user?.email,schedule],queryFn:({signal})=>api<Reservation[]>(`/api/reservations${query({horarioId:schedule})}`,{signal}),enabled:canList});
  const create=useMutation({mutationFn:(body:unknown)=>write<Reservation>('/api/reservations','POST',body),onSuccess:r=>{void client.invalidateQueries({queryKey:['operation-reservations']});navigate(`/panel/reservas/${r.id}`);}});
  return <><Heading title={user?.role==='GUIDE'?'Consulta una reserva asignada':'Administración de reservas'}>Acceso limitado por rol y pertenencia. El servidor verifica cada consulta.</Heading><div className="two-panels"><Section title="Abrir una reserva conocida"><form onSubmit={e=>{e.preventDefault();navigate(`/panel/reservas/${new FormData(e.currentTarget).get('id')}`);}}><Field label="ID de reserva"><input name="id" type="number" min={1} required step={1}/></Field><button className="button">Abrir detalle</button></form></Section>{user?.role!=='GUIDE'&&<Section title="Consultar por salida"><form onSubmit={e=>{e.preventDefault();setParams({horarioId:String(new FormData(e.currentTarget).get('horarioId'))});}}><Field label="ID de una salida de tu agencia"><input type="number" name="horarioId" min={1} required step={1} defaultValue={schedule}/></Field><button className="button secondary">Consultar reservas</button></form></Section>}</div>{user?.role==='GUIDE'&&<Notice>No hay un listado global para guías. Abre una reserva conocida o utiliza su QR, y el servidor comprobará que la salida está asignada a tu cuenta.</Notice>}{canList&&<><ErrorBox error={result.error} retry={()=>void result.refetch()}/>{result.isPending?<Loading/>:result.data?.length?<ReservationTable reservations={result.data} workspace/>:!result.error&&<Empty title="No hay reservas para esta consulta"/>}</>}{user?.role==='ADMIN'&&<Section title="Crear reserva administrativa"><form onSubmit={e=>{e.preventDefault();const f=new FormData(e.currentTarget);create.mutate({turistaId:Number(f.get('turistaId')),horarioId:Number(f.get('horarioId')),cantidadPersonas:Number(f.get('cantidadPersonas')),observaciones:f.get('observaciones')||null});}}><div className="form-grid"><Field label="ID del turista activo"><input name="turistaId" type="number" min={1} required step={1}/></Field><Field label="ID de la salida"><input name="horarioId" type="number" min={1} required step={1}/></Field><Field label="Personas"><input name="cantidadPersonas" type="number" min={1} required step={1}/></Field><Field label="Observaciones · opcional"><textarea name="observaciones" maxLength={2000}/></Field></div><ErrorBox error={create.error}/><button className="button" disabled={create.isPending}>Crear solicitud pendiente</button></form></Section>}</>;
}

function QrScanner({onRead}:{onRead:(value:string)=>void}){
  const id=useId().replace(/\W/g,''),callback=useRef(onRead),[error,setError]=useState<unknown>(null);
  callback.current=onRead;
  useEffect(()=>{let disposed=false;let scanner:import('html5-qrcode').Html5QrcodeScanner|undefined;
    import('html5-qrcode').then(({Html5QrcodeScanner})=>{if(disposed)return;scanner=new Html5QrcodeScanner(id,{fps:8,qrbox:{width:230,height:230}},false);scanner.render(v=>callback.current(v),()=>{});}).catch(setError);
    return()=>{disposed=true;if(scanner)void scanner.clear().catch(()=>{});};
  },[id]);
  return <><div id={id}/><ErrorBox error={error}/><small>La cámara requiere HTTPS o localhost y permiso del navegador. Leer el QR no realiza el canje.</small></>;
}
export function CheckIn(){
  const [payload,setPayload]=useState(''),[camera,setCamera]=useState(false),client=useQueryClient();
  const mutation=useMutation({
    mutationFn:(qrPayload:string)=>write<Voucher>('/api/reservations/check-in','POST',{qrPayload}),
    onSuccess:()=>{setCamera(false);void client.invalidateQueries({queryKey:['voucher']});},
  });
  return <>
    <Heading title="Valida el acceso a la experiencia">Escanea el QR: ya no necesitas introducir el ID de la reserva.</Heading>
    <div className="two-panels">
      <Section title="Leer voucher">
        <button className="button secondary" disabled={mutation.isPending} onClick={()=>setCamera(!camera)}><Camera size={18}/> {camera?'Cerrar lector':'Abrir lector de cámara o imagen'}</button>
        {camera&&<QrScanner onRead={v=>{setPayload(v);setCamera(false);mutation.reset();}}/>}
        <form onSubmit={e=>{e.preventDefault();mutation.mutate(payload.trim());}}>
          <Field label="Contenido del QR" hint="También puedes pegar el contenido completo de un QR de Rodatours.">
            <textarea value={payload} disabled={mutation.isPending} onChange={e=>{setPayload(e.target.value);mutation.reset();}} placeholder="rodatours:reservation:…" required maxLength={150}/>
          </Field>
          <button className="button" disabled={mutation.isPending||!payload.trim()}><QrCode size={18}/>{mutation.isPending?'Validando…':'Validar y registrar acceso'}</button>
        </form>
        <Notice>Leer el QR no lo canjea automáticamente. Revisa el código y pulsa validar para registrar el acceso.</Notice>
      </Section>
      <Section title="Resultado de validación">
        <ErrorBox error={mutation.error}/>
        {mutation.data?<div className="checkin-success"><CheckCircle2 size={42}/><h2>Acceso registrado</h2><h3>{mutation.data.tour}</h3><p>RT-{mutation.data.reservationId} · {mutation.data.persons} personas</p><p>{mutation.data.checkedInAt&&dateTime(mutation.data.checkedInAt)}</p><Link to={`/panel/reservas/${mutation.data.reservationId}`}>Abrir reserva →</Link></div>:<Empty title="Esperando un voucher">El resultado sólo aparecerá después de la respuesta del servidor.</Empty>}
        <Notice>El check-in exige una reserva confirmada y salida hoy. No completa la experiencia ni modifica cupos. Un segundo canje devuelve conflicto 409.</Notice>
      </Section>
    </div>
  </>;
}

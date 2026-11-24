import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../auth';
import { api, optional, write } from '../lib/api';
import { dateTime, money } from '../lib/format';
import type { Reservation } from '../types';
import { Empty, ErrorBox, Field, Heading, Loading, Notice, Section } from '../components/ui';

interface Payment { id:number;reservationId:number;amount:number;currency:string;status:string;checkoutUrl:string|null;checkedAt:string|null;duplicateDetected:boolean;sandbox:boolean }
export function sandboxUrl(url:string){try{const u=new URL(url);return u.protocol==='https:'&&!u.username&&!u.password&&['sandbox.mercadopago.com.co','sandbox.mercadopago.com'].includes(u.hostname);}catch{return false;}}
export function Payments(){
  const [params,setParams]=useSearchParams(),{user}=useAuth(),client=useQueryClient();
  const id=Number(params.get('reservaId')),valid=Number.isSafeInteger(id)&&id>0;
  const [message,setMessage]=useState('');
  const config=useQuery({queryKey:['payment-config'],queryFn:({signal})=>api<{enabled:boolean;sandbox:boolean}>('/api/payments/config',{signal})});
  const reservation=useQuery({queryKey:['reservation',user?.email,String(id)],queryFn:({signal})=>api<Reservation>(`/api/reservations/${id}`,{signal}),enabled:valid});
  const payment=useQuery({queryKey:['payment',user?.email,id],queryFn:({signal})=>optional(api<Payment>(`/api/reservations/${id}/payment`,{signal})),enabled:valid&&!!reservation.data});
  const checkout=useMutation({mutationFn:()=>write<Payment>(`/api/reservations/${id}/payment/checkout`,'POST'),onMutate:()=>setMessage(''),onSuccess:()=>{setMessage('Checkout de pruebas preparado. El estado sólo se actualizará después de verificarlo con el proveedor.');void client.invalidateQueries({queryKey:['payment']});},onError:()=>{void client.invalidateQueries({queryKey:['payment']});}});
  const reconcile=useMutation({mutationFn:()=>write<Payment>(`/api/reservations/${id}/payment/reconcile`,'POST'),onMutate:()=>setMessage(''),onSuccess:()=>{setMessage('Consulta de conciliación completada.');void client.invalidateQueries({queryKey:['payment']});}});
  const p=payment.data;
  const states:Record<string,string>={NOT_STARTED:'Sin iniciar',CHECKOUT_UNKNOWN:'Resultado incierto: requiere revisión',CHECKOUT_READY:'Checkout preparado',PENDING:'Pendiente de pago',APPROVED:'Pago aprobado',REJECTED:'Pago rechazado',CANCELLED:'Pago cancelado',REFUNDED:'Reembolsado',PARTIALLY_REFUNDED:'Reembolso parcial',CHARGED_BACK:'Contracargo',REVIEW_REQUIRED:'Revisión administrativa requerida'};
  return <div className="container page-space narrow"><Heading eyebrow="EXCLUSIVAMENTE SANDBOX" title="Pago de tu reserva">No ingreses tarjetas ni dinero reales. Pagar no confirma ni reabre una reserva.</Heading>
    <Notice warning>El regreso desde Mercado Pago no acredita el pago. Esta pantalla ignora los estados recibidos en la URL y consulta el backend.</Notice>
    {!valid?<Section title="Selecciona una reserva"><form onSubmit={e=>{e.preventDefault();setParams({reservaId:String(new FormData(e.currentTarget).get('id'))});}}><Field label="ID de reserva"><input name="id" type="number" min={1} step={1} required/></Field><button className="button">Consultar pago</button></form></Section>:<>
      <ErrorBox error={reservation.error||payment.error||config.error}/>
      {reservation.isPending||payment.isFetching?<Loading/>:reservation.data&&<Section title={`Reserva RT-${id}`}>
        {!config.data?.enabled&&<Notice warning>El proveedor de pruebas no está configurado. No se simularán cobros ni aprobaciones.</Notice>}
        <p>Total de reserva: <strong>{money(reservation.data.precioTotal)}</strong></p>
        {p?<><h2>{states[p.status]||p.status}</h2><p>{money(p.amount)} · {p.currency} · Sandbox</p>{p.checkedAt&&<small>Última consulta: {dateTime(p.checkedAt)}</small>}{p.duplicateDetected&&<Notice warning>Se detectó más de un pago aprobado para esta referencia. Contacta al administrador; no vuelvas a pagar.</Notice>}</>:<Empty title="Esta reserva no tiene un pago iniciado"/>}
        {reservation.data.estado!=='CONFIRMED'&&<Notice>El checkout sólo puede iniciarse para una reserva confirmada y antes de la salida. Si cancelaste una reserva pagada, su revisión/reembolso es una operación separada.</Notice>}
        {message&&<Notice>{message}</Notice>}<ErrorBox error={checkout.error||reconcile.error}/>
        <div className="form-actions">
          {(user?.role==='ADMIN'||user?.role==='TOURIST')&&<button className="button" disabled={!config.data?.enabled||reservation.data.estado!=='CONFIRMED'||checkout.isPending||!!p?.checkoutUrl||['APPROVED','REVIEW_REQUIRED','REFUNDED','PARTIALLY_REFUNDED','CHARGED_BACK','CHECKOUT_UNKNOWN'].includes(p?.status||'')} onClick={()=>checkout.mutate()}>Preparar checkout sandbox</button>}
          {p?.checkoutUrl&&sandboxUrl(p.checkoutUrl)&&<a className="button" href={p.checkoutUrl} target="_blank" rel="noopener noreferrer">Abrir Mercado Pago de pruebas ↗</a>}
          {user?.role!=='GUIDE'&&<button className="button secondary" disabled={!p||!config.data?.enabled||reconcile.isPending} onClick={()=>reconcile.mutate()}>Verificar con el proveedor</button>}
        </div>
        <Link className="back-link" to={`${user?.role==='TOURIST'?'':'/panel'}/reservas/${id}`}>← Volver a la reserva</Link>
      </Section>}
    </>}
  </div>;
}

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Bell } from 'lucide-react';
import { useAuth } from '../auth';
import { api, write } from '../lib/api';
import { dateTime } from '../lib/format';
import type { Notification, Page } from '../types';
import { Empty, ErrorBox, Heading, Loading, Notice } from '../components/ui';
export function Notifications(){
  const [page,setPage]=useState(0),{user}=useAuth(),client=useQueryClient();
  const result=useQuery({queryKey:['notifications',user?.email,page],queryFn:({signal})=>api<Page<Notification>>(`/api/notifications?page=${page}&size=10`,{signal})});
  const mark=useMutation({mutationFn:(id:number)=>write(`/api/notifications/${id}/read`,'PUT'),onSuccess:()=>client.invalidateQueries({queryKey:['notifications']})});
  return <div className="container page-space narrow"><Heading eyebrow="SIEMPRE AL DÍA" title="Tus notificaciones">Un espacio privado para seguir tus reservas.</Heading><ErrorBox error={result.error} retry={()=>void result.refetch()}/><ErrorBox error={mark.error}/>{result.isPending?<Loading/>:result.data?.content.length?<div className="notification-list">{result.data.content.map(n=><article className={`panel notification ${n.readAt?'read':''}`} key={n.id}><span className="notification-icon"><Bell size={21}/></span><div><small>{dateTime(n.createdAt)}</small><h2>{n.subject}</h2><p>{n.message}</p><div className="form-actions"><Link to={`${user?.role==='TOURIST'?'':'/panel'}/reservas/${n.reservationId}`}>Ver reserva →</Link>{!n.readAt&&<button className="text-button" disabled={mark.isPending} onClick={()=>mark.mutate(n.id)}>Marcar como leída</button>}</div></div></article>)}</div>:!result.error&&<Empty title="Todo está al día"/>}<div className="pagination"><button className="button secondary" disabled={!page} onClick={()=>setPage(page-1)}>Anterior</button><span>Página {page+1}</span><button className="button secondary" disabled={!result.data||result.data.last} onClick={()=>setPage(page+1)}>Siguiente</button></div><Notice>Los avisos permanecen aquí aunque el correo no llegue. La entrega SMTP requiere configuración del servidor.</Notice></div>;
}

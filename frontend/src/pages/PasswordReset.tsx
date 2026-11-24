import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { write } from '../lib/api';
import { ErrorBox, Field, Heading, Notice, Section } from '../components/ui';

export function PasswordReset(){
  const [token]=useState(()=>new URLSearchParams(window.location.hash.slice(1)).get('token')||'');
  useEffect(()=>{if(window.location.hash)window.history.replaceState(null,'',window.location.pathname);},[]);
  const request=useMutation({mutationFn:(email:string)=>write<{message:string}>('/api/auth/password-reset/request','POST',{email})});
  const confirm=useMutation({mutationFn:(password:string)=>write('/api/auth/password-reset/confirm','POST',{token,password}),onSuccess:()=>window.dispatchEvent(new Event('rodatours:unauthorized'))});
  return <div className="container page-space narrow"><Heading title={token?'Elige una nueva contraseña':'Recupera tu acceso'}>No compartas tu enlace de recuperación; vence en 30 minutos y sirve una sola vez.</Heading><Section>
    {token?<form onSubmit={e=>{e.preventDefault();const f=new FormData(e.currentTarget);confirm.mutate(String(f.get('password')));}}>
      <Field label="Nueva contraseña" hint="Al menos 12 caracteres; máximo 72 bytes UTF-8."><input name="password" type="password" autoComplete="new-password" minLength={12} required onChange={e=>e.target.setCustomValidity(new TextEncoder().encode(e.target.value).length>72?'Máximo 72 bytes UTF-8.':'')}/></Field>
      <ErrorBox error={confirm.error}/>{confirm.isSuccess?<Notice>Contraseña cambiada. Las sesiones anteriores ya no son válidas. <Link to="/acceso">Inicia sesión nuevamente.</Link></Notice>:<button className="button" disabled={confirm.isPending}>Cambiar contraseña</button>}
    </form>:<form onSubmit={e=>{e.preventDefault();request.mutate(String(new FormData(e.currentTarget).get('email')));}}>
      <Field label="Correo electrónico"><input name="email" type="email" autoComplete="email" required maxLength={255}/></Field><ErrorBox error={request.error}/>{request.data&&<Notice>{request.data.message}</Notice>}<button className="button" disabled={request.isPending}>Solicitar instrucciones</button>
    </form>}
  </Section><Link to="/acceso">← Volver a iniciar sesión</Link></div>;
}

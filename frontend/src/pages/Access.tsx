import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ArrowRight, ShieldCheck } from 'lucide-react';
import { useAuth } from '../auth';
import { write } from '../lib/api';
import { Coast, ErrorBox, Field, Notice } from '../components/ui';

const loginSchema=z.object({email:z.email('Escribe un correo válido.'),password:z.string().min(1,'Escribe tu contraseña.')});
export const registrationSchema=loginSchema.extend({name:z.string().trim().min(1,'Escribe tu nombre.').max(150),phone:z.string().max(20),password:z.string().min(12,'Usa al menos 12 caracteres.').refine(v=>new TextEncoder().encode(v).length<=72,'Máximo 72 bytes UTF-8.')});
type Values=z.infer<typeof registrationSchema>;
export function Access({registering=false}:{registering?:boolean}){
  const {user,login}=useAuth(),navigate=useNavigate(),location=useLocation();
  const [error,setError]=useState<unknown>(null);
  const {register,handleSubmit,formState:{errors,isSubmitting},reset}=useForm<Values>({resolver:zodResolver(registrationSchema),defaultValues:{name:registering?'':'No aplica',phone:''}});
  const from=typeof location.state?.from==='string'&&location.state.from.startsWith('/')&&!location.state.from.startsWith('//')?location.state.from:null;
  // El esquema de login no impone la política de alta a contraseñas existentes.
  const loginForm=useForm<z.infer<typeof loginSchema>>({resolver:zodResolver(loginSchema)});
  if(user)return <Navigate to={from||(user.role==='TOURIST'?'/reservas':'/panel')} replace/>;
  async function submit(v:z.infer<typeof loginSchema>&Partial<Values>){setError(null);try{if(registering)await write('/api/auth/register','POST',{name:v.name,email:v.email,password:v.password,phone:v.phone||null});const identity=await login(v.email,v.password);reset();loginForm.reset();navigate(from||(identity.role==='TOURIST'?'/reservas':'/panel'),{replace:true});}catch(e){setError(e);}}
  const fields=(name:'email'|'password')=>registering?register(name):loginForm.register(name);
  return <div className="container auth-layout"><div className="auth-story"><Coast/><div><span className="eyebrow light">TU PRÓXIMA HISTORIA EMPIEZA AQUÍ</span><h1>El Caribe se vive.<br/>No se explica.</h1><p>Conecta con experiencias locales y descubre otra forma de viajar.</p><span className="auth-trust"><ShieldCheck size={20}/> Tus reservas, en un solo lugar</span></div></div><section className="auth-form panel"><span className="eyebrow">BIENVENIDO A RODATOURS</span><h2>{registering?'Crea tu cuenta de viajero':'Qué bueno verte de nuevo.'}</h2><p>{registering?'Regístrate y empieza a descubrir Santa Marta.':'Entra para continuar con tu próxima aventura.'}</p><ErrorBox error={error}/><form onSubmit={registering?handleSubmit(submit):loginForm.handleSubmit(submit)}>{registering&&<Field label="Nombre completo"><input {...register('name')} autoComplete="name" maxLength={150}/>{errors.name&&<small className="field-error">{errors.name.message}</small>}</Field>}<Field label="Correo electrónico"><input {...fields('email')} type="email" autoComplete="email" required maxLength={255}/>{(errors.email||loginForm.formState.errors.email)&&<small className="field-error">{(errors.email||loginForm.formState.errors.email)?.message}</small>}</Field>{registering&&<Field label="Teléfono · opcional"><input {...register('phone')} type="tel" autoComplete="tel" maxLength={20}/></Field>}<Field label="Contraseña" hint={registering?'Al menos 12 caracteres y máximo 72 bytes UTF-8.':undefined}><input {...fields('password')} type="password" autoComplete={registering?'new-password':'current-password'} required/>{(errors.password||loginForm.formState.errors.password)&&<small className="field-error">{(errors.password||loginForm.formState.errors.password)?.message}</small>}</Field><button className="button full" disabled={isSubmitting||loginForm.formState.isSubmitting}>{isSubmitting||loginForm.formState.isSubmitting?'Conectando…':registering?'Crear mi cuenta':'Iniciar sesión'}<ArrowRight size={18}/></button></form><div className="auth-switch">{registering?'¿Ya tienes una cuenta?':'¿Es tu primera vez aquí?'} <Link to={registering?'/acceso':'/registro'} state={location.state}>{registering?'Inicia sesión':'Regístrate'}</Link></div><Notice>{registering?'El registro público sólo crea cuentas de turista.':'Por seguridad, tu acceso no se guarda al recargar la página. No hay recuperación de contraseña en esta etapa.'}</Notice></section></div>;
}

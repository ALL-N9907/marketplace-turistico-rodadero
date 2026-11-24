import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { api, clearCredentials, write } from './lib/api';
import { ErrorBox, Loading } from './components/ui';
import type { Identity, Role } from './types';

interface Auth { user: Identity | null; loading: boolean; login: (email: string, password: string) => Promise<Identity>; logout: () => Promise<void> }
const publicKeys=new Set(['catalog','public-agencies','agencies-directory','types','tour','tour-details','video','agency','agency-location','tour-gallery','public-reviews','compare','availability']);
const privateQuery=(query:{queryKey:readonly unknown[]})=>!publicKeys.has(String(query.queryKey[0]));
const Context = createContext<Auth | null>(null);
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Identity | null>(null);
  const [loading,setLoading]=useState(true),[error,setError]=useState<unknown>(null);
  const generation=useRef(0);
  const currentUser=useRef(user);currentUser.current=user;
  const client = useQueryClient();
  useEffect(()=>{
    const expired=()=>{++generation.current;clearCredentials();setUser(null);setLoading(false);if(currentUser.current){void client.cancelQueries({predicate:privateQuery});client.removeQueries({predicate:privateQuery});}};
    window.addEventListener('rodatours:unauthorized',expired);
    return()=>window.removeEventListener('rodatours:unauthorized',expired);
  },[client]);
  useEffect(()=>{
    const controller=new AbortController(),version=++generation.current;
    api<Identity>('/api/auth/me',{signal:controller.signal}).then(identity=>{
      if(generation.current===version)setUser(identity);
    }).catch(()=>{}).finally(()=>{if(!controller.signal.aborted&&generation.current===version)setLoading(false);});
    return()=>{controller.abort();};
  },[]);
  async function login(email: string, password: string) {
    ++generation.current;setLoading(false);setError(null);
    await client.cancelQueries({predicate:privateQuery});client.removeQueries({predicate:privateQuery});setUser(null);
    clearCredentials();
    try { const identity=await write<Identity>('/api/auth/login','POST',{email,password}); setUser(identity); return identity; }
    catch (e) { clearCredentials(); throw e; }
  }
  async function logout() {
    setError(null);
    try { await write('/api/auth/logout','POST');++generation.current;clearCredentials();setUser(null);await client.cancelQueries({predicate:privateQuery});client.removeQueries({predicate:privateQuery}); }
    catch(e){setError(e);}
  }
  return <Context.Provider value={{ user, loading, login, logout }}>{!!error&&<ErrorBox error={error}/>}{children}</Context.Provider>;
}
export function useAuth() { const value = useContext(Context); if (!value) throw new Error('AuthProvider requerido'); return value; }
export function Protected({ roles }: { roles?: Role[] }) {
  const { user,loading } = useAuth(), location = useLocation();
  if(loading)return <div className="container page-space"><Loading/></div>;
  if (!user) return <Navigate to="/acceso" replace state={{ from: location.pathname + location.search }} />;
  if (roles && !roles.includes(user.role)) return <section className="empty"><h1>Este espacio no corresponde a tu rol</h1><p>La autorización también se verifica en el servidor.</p><a href="/">Volver a explorar</a></section>;
  return <Outlet />;
}

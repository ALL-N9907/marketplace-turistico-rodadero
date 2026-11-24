import { createContext, useContext, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { api, clearCredentials, setCredentials } from './lib/api';
import type { Identity, Role } from './types';

interface Auth { user: Identity | null; login: (email: string, password: string) => Promise<Identity>; logout: () => void }
const Context = createContext<Auth | null>(null);
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Identity | null>(null);
  const client = useQueryClient();
  async function login(email: string, password: string) {
    await client.cancelQueries(); client.clear(); setUser(null);
    setCredentials(email, password);
    try { const identity=await api<Identity>('/api/auth/me'); setUser(identity); return identity; }
    catch (e) { clearCredentials(); throw e; }
  }
  function logout() { clearCredentials(); setUser(null); void client.cancelQueries(); client.clear(); }
  return <Context.Provider value={{ user, login, logout }}>{children}</Context.Provider>;
}
export function useAuth() { const value = useContext(Context); if (!value) throw new Error('AuthProvider requerido'); return value; }
export function Protected({ roles }: { roles?: Role[] }) {
  const { user } = useAuth(), location = useLocation();
  if (!user) return <Navigate to="/acceso" replace state={{ from: location.pathname + location.search }} />;
  if (roles && !roles.includes(user.role)) return <section className="empty"><h1>Este espacio no corresponde a tu rol</h1><p>La autorización también se verifica en el servidor.</p><a href="/">Volver a explorar</a></section>;
  return <Outlet />;
}

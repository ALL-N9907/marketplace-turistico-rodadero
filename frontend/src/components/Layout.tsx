import { useState } from 'react';
import { Bell, Compass, LayoutDashboard, LogOut, Menu, X } from 'lucide-react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth';
import { roleLabels } from '../lib/format';
import type { Role } from '../types';

export const workspaceLinks: { to: string; label: string; roles: Role[] }[] = [
  { to: '/panel', label: 'Resumen', roles: ['ADMIN','AGENCY_OPERATOR','GUIDE','COORDINATOR'] },
  { to: '/panel/tours', label: 'Tours y programación', roles: ['ADMIN','AGENCY_OPERATOR'] },
  { to: '/panel/reservas', label: 'Reservas', roles: ['ADMIN','AGENCY_OPERATOR','GUIDE'] },
  { to: '/panel/validar', label: 'Validar QR', roles: ['ADMIN','AGENCY_OPERATOR','GUIDE'] },
  { to: '/panel/agencias', label: 'Agencias', roles: ['ADMIN'] },
  { to: '/panel/perfil', label: 'Perfil de agencia', roles: ['AGENCY_OPERATOR'] },
  { to: '/panel/guias', label: 'Guías', roles: ['ADMIN','AGENCY_OPERATOR'] },
  { to: '/panel/turistas', label: 'Turistas', roles: ['ADMIN'] },
  { to: '/panel/categorias', label: 'Tipos de experiencia', roles: ['ADMIN'] },
  { to: '/panel/cuentas', label: 'Cuentas de personal', roles: ['ADMIN'] },
  { to: '/panel/reportes', label: 'Reportes', roles: ['ADMIN','AGENCY_OPERATOR','COORDINATOR'] },
];
export function Brand() { return <Link className="brand" to="/" aria-label="Rodatours, inicio"><span className="brand-icon"><Compass size={23}/></span><span><b>roda</b>tours<span className="brand-dot">.</span></span></Link>; }
export function PublicLayout() {
  const { user, logout } = useAuth(); const [open, setOpen] = useState(false);
  return <><a className="skip-link" href="#contenido">Saltar al contenido</a><header className="site-header"><div className="header-inner"><Brand/><button className="icon-button menu-toggle" aria-label="Abrir navegación" aria-expanded={open} onClick={()=>setOpen(!open)}>{open?<X/>:<Menu/>}</button><nav className={open?'public-nav open':'public-nav'} aria-label="Navegación principal" onClick={()=>setOpen(false)}><NavLink to="/" end>Explorar</NavLink><NavLink to="/agencias">Agencias</NavLink>{user?.role==='TOURIST' && <NavLink to="/reservas">Mis reservas</NavLink>}{user && user.role!=='TOURIST' && <NavLink to="/panel">Mi panel</NavLink>}</nav><div className="header-actions">{user ? <><Link to="/notificaciones" className="icon-button" aria-label="Notificaciones"><Bell size={21}/></Link><button className="button secondary small" onClick={logout}><LogOut size={16}/> Salir</button></> : <Link to="/acceso" className="button small">Iniciar sesión <span>↗</span></Link>}</div></div></header><main id="contenido" className="public-main"><Outlet/></main><footer className="site-footer"><Brand/><p>El Rodadero · Santa Marta, Colombia</p><span>Experiencias locales. Historias inolvidables.</span></footer><nav className="mobile-bottom" aria-label="Navegación móvil"><NavLink to="/" end>Explorar</NavLink><NavLink to="/agencias">Agencias</NavLink><NavLink to={user?.role==='TOURIST'?'/reservas':user?'/panel':'/acceso'}>{user?.role==='TOURIST'?'Reservas':user?'Mi panel':'Acceso'}</NavLink>{user && <NavLink to="/notificaciones">Avisos</NavLink>}</nav></>;
}
export function WorkspaceLayout() {
  const { user, logout } = useAuth(); const [open,setOpen]=useState(false);
  if (!user) return null;
  return <div className="workspace"><a className="skip-link" href="#contenido">Saltar al contenido</a><aside className={`sidebar ${open?'open':''}`}><Brand/><p className="sidebar-caption">ESPACIO DE OPERACIÓN</p><nav aria-label="Panel de gestión" onClick={()=>setOpen(false)}>{workspaceLinks.filter(l=>l.roles.includes(user.role)).map(l=><NavLink key={l.to} to={l.to} end={l.to==='/panel'}>{l.to==='/panel'&&<LayoutDashboard size={18}/>} {l.label}</NavLink>)}</nav><div className="sidebar-bottom"><small>{roleLabels[user.role]}</small><span>{user.email}</span><Link to="/">← Explorar experiencias</Link><button onClick={logout}><LogOut size={16}/> Cerrar sesión</button></div></aside><div className="workspace-body"><header className="workspace-header"><button className="icon-button menu-toggle" aria-label="Abrir panel" aria-expanded={open} onClick={()=>setOpen(!open)}><Menu/></button><span>El Rodadero <span className="muted">/ Mi espacio</span></span><Link to="/notificaciones" className="button secondary small"><Bell size={17}/> Notificaciones</Link></header><main id="contenido" className="workspace-main"><Outlet/></main></div></div>;
}

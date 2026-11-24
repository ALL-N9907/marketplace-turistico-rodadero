import { useId, type ReactNode } from 'react';
import { AlertCircle, ArrowRight, Compass, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';
import { ApiError } from '../lib/api';
import { money, statusLabels } from '../lib/format';
import type { CatalogEntry, Status } from '../types';

export function Heading({ eyebrow, title, children }: { eyebrow?: string; title: string; children?: ReactNode }) {
  return <div className="page-heading">{eyebrow && <span className="eyebrow">{eyebrow}</span>}<h1>{title}</h1>{children && <p>{children}</p>}</div>;
}
export function Notice({ children, warning = false }: { children: ReactNode; warning?: boolean }) { return <div className={`notice ${warning ? 'warning' : ''}`}>{children}</div>; }
export function ErrorBox({ error, retry }: { error: unknown; retry?: () => void }) {
  if (!error) return null;
  return <div className="error-box" role="alert"><AlertCircle size={20} /><div><strong>{error instanceof ApiError && error.status === 409 ? 'La información cambió. Actualiza antes de continuar.' : 'No pudimos completar la operación'}</strong><p>{error instanceof Error ? error.message : 'Ocurrió un error inesperado.'}</p>{error instanceof ApiError && Object.entries(error.errors).map(([k,v]) => <p key={k}>{k}: {String(v)}</p>)}{retry && <button className="button secondary small" onClick={retry}><RefreshCw size={15} /> Reintentar</button>}</div></div>;
}
export function Loading() { return <div className="skeletons" role="status" aria-label="Cargando información"><div /><div /><div /><span>Cargando información…</span></div>; }
export function Empty({ title = 'Todavía no hay resultados', children }: { title?: string; children?: ReactNode }) { return <div className="empty"><Compass size={36} /><h2>{title}</h2><p>{children || 'Prueba otros filtros o vuelve más tarde.'}</p></div>; }
export function Badge({ status }: { status: string }) { return <span className={`badge ${status.toLowerCase()}`}>{statusLabels[status as Status] || ({ ACTIVE: 'Activo', INACTIVE: 'Inactivo' }[status]) || status}</span>; }
export function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) { return <label className="field"><span>{label}</span>{children}{hint && <small>{hint}</small>}</label>; }
export function Coast({ variant = 0, className = '' }: { variant?: number; className?: string }) {
  const id = useId();
  return <svg className={`coast ${className}`} viewBox="0 0 600 320" role="img" aria-label="Ilustración de la costa y las montañas de Santa Marta" preserveAspectRatio="xMidYMid slice"><defs><linearGradient id={id} x2="0" y2="1"><stop stopColor={variant % 2 ? '#e4eac5' : '#c9eee7'} /><stop offset="1" stopColor="#95d5cc" /></linearGradient></defs><path fill={`url(#${id})`} d="M0 0h600v320H0z"/><circle cx="465" cy="66" r="34" fill="#ffd69b"/><path d="M0 229L137 58l164 164 119-124 180 132v90H0Z" fill="#90bbaa"/><path d="M0 263L193 139l183 119 130-64 94 59v67H0Z" fill="#347e70"/><path d="M0 260q150-72 322-2t278-17v79H0Z" fill="#3da5a5"/><path d="M0 304q180-58 335-12t265-7v35H0Z" fill="#f2dfb7"/><path d="M0 284q155-50 322-1t278-18" stroke="#edfffa" strokeWidth="3" fill="none"/><path d="M66 216q-4-47 17-64m-8 18q-33-14-42 1m42-1q24-16 39-2" fill="none" stroke="#1f5b4d" strokeWidth="6" strokeLinecap="round"/></svg>;
}
export function TourCard({ entry, agency, selected, onSelect }: { entry: CatalogEntry; agency?: string; selected?: boolean; onSelect?: () => void }) {
  const { tour, details } = entry;
  return <article className="tour-card"><Link to={`/tours/${tour.id}`} className="card-art" aria-label={`Ver ${tour.titulo}`}><Coast variant={tour.id} /><span className="art-label">{tour.destino}</span></Link><div className="card-body"><div className="card-kicker">{agency || `Agencia #${tour.agenciaId}`}</div><h3><Link to={`/tours/${tour.id}`}>{tour.titulo}</Link></h3><div className="tour-meta">{tour.duracionHoras} horas <span>·</span> Dificultad {tour.nivelDificultad.toLowerCase()}</div>{details.supportsCommunities && <span className="sustainability">Apoyo a comunidades · declarado</span>}<div className="card-bottom"><div><strong>{money(tour.precio)}</strong><small>por persona</small></div><Link to={`/tours/${tour.id}`} className="circle-link" aria-label={`Explorar ${tour.titulo}`}><ArrowRight size={21}/></Link></div>{onSelect && <label className="check compare-check"><input type="checkbox" checked={selected} onChange={onSelect}/> Comparar esta experiencia</label>}</div></article>;
}
export function Section({ title, children, className = '' }: { title?: string; children: ReactNode; className?: string }) { return <section className={`panel ${className}`}>{title && <h2>{title}</h2>}{children}</section>; }

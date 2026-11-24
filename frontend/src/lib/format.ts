import type { Identity, Reservation } from '../types';
export const money = (value: number) => new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(value) + ' COP';
export const dateTime = (value: string) => new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
export const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; };
export const statusLabels = { SCHEDULED: 'Pendiente', CONFIRMED: 'Confirmada', CANCELLED: 'Cancelada', COMPLETED: 'Completada', NO_SHOW: 'No asistió' };
export const roleLabels = { ADMIN: 'Administrador', AGENCY_OPERATOR: 'Operador de agencia', GUIDE: 'Guía', COORDINATOR: 'Coordinador', TOURIST: 'Viajero' };
export function reservationActions(user: Identity, reservation: Reservation) {
  const active = ['SCHEDULED', 'CONFIRMED'].includes(reservation.estado);
  const admin = user.role === 'ADMIN', operator = user.role === 'AGENCY_OPERATOR';
  return {
    confirm: (admin || operator) && reservation.estado === 'SCHEDULED',
    cancel: (admin || operator || (user.role === 'TOURIST' && user.touristId === reservation.turistaId)) && active,
    complete: (admin || user.role === 'GUIDE') && reservation.estado === 'CONFIRMED',
    noShow: admin && reservation.estado === 'CONFIRMED',
    review: user.role === 'TOURIST' && user.touristId === reservation.turistaId && reservation.estado === 'COMPLETED',
    checkIn: (admin || operator || user.role === 'GUIDE') && reservation.estado === 'CONFIRMED',
  }; // La API sigue siendo la autoridad para propiedad, guía asignado y fecha operativa.
}

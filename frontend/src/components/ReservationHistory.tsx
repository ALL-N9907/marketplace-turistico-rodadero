import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../auth';
import { api } from '../lib/api';
import { dateTime, roleLabels, statusLabels } from '../lib/format';
import type { HistoryPage } from '../types';
import { Badge, Empty, ErrorBox, Loading, Notice, Section } from './ui';

export function ReservationHistory({ reservationId }: { reservationId: number }) {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const result = useQuery({
    queryKey: ['reservation-history', user?.email, reservationId, page],
    queryFn: ({ signal }) => api<HistoryPage>(`/api/reservations/${reservationId}/history?page=${page}&size=20`, { signal }),
  });
  return <Section title="Historial de estados">
    <Notice>Se registran los cambios realizados desde la incorporación del historial. No se reconstruyen cambios anteriores ni se incluyen ediciones de notas o check-in.</Notice>
    <ErrorBox error={result.error} retry={() => void result.refetch()} />
    {result.isPending ? <Loading /> : result.data && <>
      {result.data.content.length ? <ol className="reservation-timeline">{result.data.content.map(event => <li key={event.id}>
        <div className="timeline-state">
          <span>{event.previousStatus ? `${statusLabels[event.previousStatus]} →` : 'Reserva creada →'}</span>
          <Badge status={event.status} />
        </div>
        <small>{dateTime(event.occurredAt)} · {event.actorRole === 'SYSTEM' ? 'Sistema' : roleLabels[event.actorRole]}</small>
        {event.reason && <p className="preserve-lines">{event.reason}</p>}
      </li>)}</ol> : <Empty title="Sin cambios registrados">La reserva puede ser anterior a la incorporación del historial.</Empty>}
      {result.data.totalPages > 1 && <div className="pagination">
        <button className="button secondary" disabled={page === 0 || result.isFetching} onClick={() => setPage(page - 1)}>Anterior</button>
        <span>Página {page + 1}</span>
        <button className="button secondary" disabled={page + 1 >= result.data.totalPages || result.isFetching} onClick={() => setPage(page + 1)}>Siguiente</button>
      </div>}
    </>}
  </Section>;
}

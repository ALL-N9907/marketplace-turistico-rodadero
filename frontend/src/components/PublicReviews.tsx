import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Star } from 'lucide-react';
import { api } from '../lib/api';
import { dateTime } from '../lib/format';
import type { PublicReviewPage } from '../types';
import { Empty, ErrorBox, Loading, Section } from './ui';

export function PublicReviews({ tourId }: { tourId: number }) {
  const [page, setPage] = useState(0);
  const result = useQuery({
    queryKey: ['public-reviews', tourId, page],
    queryFn: ({ signal }) => api<PublicReviewPage>(`/api/tours/${tourId}/reviews?page=${page}&size=5`, { signal }),
  });
  return <Section title="Lo que cuentan los viajeros">
    <p>Reseñas de reservas completadas. Los comentarios son públicos: evita incluir datos personales.</p>
    <ErrorBox error={result.error} retry={() => void result.refetch()} />
    {result.isPending ? <Loading /> : result.data && <>
      <div className="review-summary">
        <Star size={26} aria-hidden="true" />
        <strong>{result.data.averageRating === null ? 'Sin calificación' : `${result.data.averageRating.toLocaleString('es-CO', { maximumFractionDigits: 1 })} / 5`}</strong>
        <span>{result.data.totalElements} reseñas</span>
      </div>
      {result.data.content.length ? result.data.content.map(review => <article className="public-review" key={review.id}>
        <span className="review-stars" aria-label={`${review.rating} de 5 estrellas`}>{'★'.repeat(review.rating)}</span>
        <small>{dateTime(review.createdAt)}</small>
        <p className="preserve-lines">{review.comment || 'Este viajero dejó una calificación sin comentario.'}</p>
      </article>) : <Empty title="Esta experiencia aún no tiene reseñas">Cuando los viajeros completen su reserva podrán compartir su experiencia.</Empty>}
      {result.data.totalPages > 1 && <div className="pagination">
        <button className="button secondary" disabled={page === 0 || result.isFetching} onClick={() => setPage(page - 1)}>Reseñas anteriores</button>
        <span>Página {page + 1}</span>
        <button className="button secondary" disabled={page + 1 >= result.data.totalPages || result.isFetching} onClick={() => setPage(page + 1)}>Más reseñas</button>
      </div>}
    </>}
  </Section>;
}

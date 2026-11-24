import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../auth';
import { PublicReviews } from './PublicReviews';
import { ReservationHistory } from './ReservationHistory';
import type { ReactNode } from 'react';

function renderWithClient(children: ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}><AuthProvider>{children}</AuthProvider></QueryClientProvider>);
}
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

describe('reseñas públicas e historial', () => {
  beforeEach(() => vi.stubGlobal('fetch', vi.fn()));
  afterEach(() => vi.unstubAllGlobals());

  it('no confunde ausencia de reseñas con una calificación de cero', async () => {
    vi.mocked(fetch).mockResolvedValue(json({ content: [], totalElements: 0, totalPages: 0, averageRating: null }));
    renderWithClient(<PublicReviews tourId={1} />);
    expect(await screen.findByText('Sin calificación')).toBeVisible();
    expect(screen.getByText('Esta experiencia aún no tiene reseñas')).toBeVisible();
    expect(screen.queryByText('0 / 5')).not.toBeInTheDocument();
  });

  it('presenta el comentario como texto, nunca como HTML ejecutable', async () => {
    const comment = '<img src=x onerror=alert(1) />';
    vi.mocked(fetch).mockResolvedValue(json({ content: [{ id: 1, rating: 5, comment, createdAt: '2026-11-23T12:00:00' }], totalElements: 1, totalPages: 1, averageRating: 5 }));
    renderWithClient(<PublicReviews tourId={1} />);
    expect(await screen.findByText(comment)).toBeVisible();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(screen.getByLabelText('5 de 5 estrellas')).toBeVisible();
  });

  it('pagina usando el servidor y conserva el promedio global', async () => {
    vi.mocked(fetch).mockImplementation(async path => {
      const next = String(path).includes('page=1');
      return json({ content: [{ id: next ? 2 : 1, rating: 4, comment: next ? 'Segunda página' : 'Primera página', createdAt: '2026-11-23T12:00:00' }], totalElements: 6, totalPages: 2, averageRating: 4.5 });
    });
    renderWithClient(<PublicReviews tourId={1} />);
    await screen.findByText('Primera página');
    fireEvent.click(screen.getByRole('button', { name: 'Más reseñas' }));
    expect(await screen.findByText('Segunda página')).toBeVisible();
    expect(screen.getByText('4,5 / 5')).toBeVisible();
  });

  it('muestra fallos de reseñas sin inventar una calificación', async () => {
    vi.mocked(fetch).mockResolvedValue(json({ detail: 'Tour no disponible.' }, 404));
    renderWithClient(<PublicReviews tourId={1} />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Tour no disponible.');
    expect(screen.queryByText('Sin calificación')).not.toBeInTheDocument();
  });

  it('explica el historial ausente sin reconstruir cambios ficticios', async () => {
    vi.mocked(fetch).mockResolvedValue(json({ content: [], totalElements: 0, totalPages: 0 }));
    renderWithClient(<ReservationHistory reservationId={1} />);
    expect(await screen.findByText('Sin cambios registrados')).toBeVisible();
    expect(screen.queryByText('Reserva creada →')).not.toBeInTheDocument();
  });

  it('muestra transición, fecha, rol y motivo sin identidad de contacto', async () => {
    vi.mocked(fetch).mockResolvedValue(json({ content: [{ id: 1, previousStatus: 'CONFIRMED', status: 'CANCELLED', occurredAt: '2026-11-23T12:00:00', actorRole: 'TOURIST', reason: 'Cambio de planes' }], totalElements: 1, totalPages: 1 }));
    renderWithClient(<ReservationHistory reservationId={1} />);
    expect(await screen.findByText('Cambio de planes')).toBeVisible();
    expect(screen.getByText('Confirmada →')).toBeVisible();
    expect(screen.getByText('Cancelada')).toBeVisible();
    expect(screen.getByText(/Viajero/)).toBeVisible();
    expect(document.body.textContent).not.toContain('@');
  });
});

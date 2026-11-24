export class ApiError extends Error {
  constructor(public status: number, message: string, public errors: Record<string, string> = {}) { super(message); this.name = 'ApiError'; }
}

// Sólo memoria. Nunca localStorage/sessionStorage, cookies propias o URLs con contraseñas.
let authorization: string | undefined;
export function setCredentials(email: string, password: string) {
  const bytes = new TextEncoder().encode(`${email}:${password}`);
  authorization = `Basic ${btoa(Array.from(bytes, b => String.fromCharCode(b)).join(''))}`;
}
export function clearCredentials() { authorization = undefined; }

async function response(path: string, init: RequestInit = {}) {
  if (!path.startsWith('/api/')) throw new Error('La API debe usar una ruta local /api/.');
  const headers = new Headers(init.headers);
  if (!headers.has('Accept')) headers.set('Accept', 'application/json, application/problem+json');
  if (authorization) headers.set('Authorization', authorization);
  let result: Response;
  try { result = await fetch(path, { ...init, headers, credentials: 'same-origin', cache: 'no-store' }); }
  catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error;
    throw new ApiError(0, 'No pudimos conectar con el servidor. Comprueba que Spring Boot esté disponible.');
  }
  if (!result.ok) {
    const problem = await result.json().catch(() => ({}));
    throw new ApiError(result.status, problem.detail || problem.title || `Error del servidor (${result.status}).`, problem.errors || {});
  }
  return result;
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method || 'GET').toUpperCase();
  const headers = new Headers(init.headers);
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    // Un token fresco conserva la cookie y evita reutilizar tokens de otra sesión.
    const csrf = await (await response('/api/auth/csrf', { signal: init.signal })).json();
    headers.set(csrf.headerName, csrf.token);
    if (init.body) headers.set('Content-Type', 'application/json');
  }
  const result = await response(path, { ...init, method, headers });
  if (result.status === 204) return undefined as T;
  if (!result.headers.get('content-type')?.includes('json')) throw new ApiError(502, 'La respuesta no es JSON. Revisa el proxy /api y el backend.');
  return result.json();
}
export const write = <T,>(path: string, method: string, body?: unknown) => api<T>(path, { method, body: body === undefined ? undefined : JSON.stringify(body) });
export async function apiBlob(path: string, signal?: AbortSignal) { return (await response(path, { signal, headers: { Accept: 'image/png, application/problem+json' } })).blob(); }
export function query(params: Record<string, string | number | boolean | null | undefined>) {
  const result = new URLSearchParams();
  Object.entries(params).forEach(([k,v]) => { if (v !== '' && v !== undefined && v !== null) result.set(k, String(v)); });
  return result.size ? `?${result}` : '';
}
export async function optional<T>(promise: Promise<T>): Promise<T | null> {
  try { return await promise; } catch (e) { if (e instanceof ApiError && e.status === 404) return null; throw e; }
}

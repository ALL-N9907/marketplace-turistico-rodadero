export type Role = 'ADMIN' | 'AGENCY_OPERATOR' | 'GUIDE' | 'COORDINATOR' | 'TOURIST';
export interface Identity { email: string; role: Role; agencyId: number | null; touristId: number | null; guideId: number | null }
export type Status = 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW';
export interface Tour { id: number; agenciaId: number; tipoExperienciaId: number; titulo: string; descripcion: string; destino: string; precio: number; duracionHoras: number; nivelDificultad: 'BAJO' | 'MEDIO' | 'ALTO'; serviciosIncluidos: string | null; estado: string }
export interface TourDetails { itinerary: string | null; conditions: string | null; supportsCommunities: boolean; environmentalPractices: boolean }
export interface CatalogEntry { tour: Tour; details: TourDetails }
export interface CatalogPage { content: CatalogEntry[]; totalElements: number; page: number; size: number }
export interface Agency { id: number; nombreComercial: string; nitORegistro: string; descripcion: string | null; direccion: string; telefonoContacto: string; estado: string }
export interface AgencyLocation { latitude: number | null; longitude: number | null; photoUrl: string | null }
export interface ExperienceType { id: number; nombre: string; descripcion: string | null }
export interface Schedule { id: number; tourId: number; guiaId: number | null; fechaTour: string; horaInicio: string; fechaFin: string; cuposTotales: number; cuposDisponibles: number }
export interface Slot { horarioId: number; tourId: number; fechaTour: string; horaInicio: string; fechaFin: string; cuposTotales: number; cuposDisponibles: number }
export interface Reservation { id: number; turistaId: number; horarioId: number; fechaReserva: string; fechaInicio: string; fechaFin: string; cantidadPersonas: number; precioTotal: number; estado: Status; motivoCancelacion: string | null; observaciones: string | null; codigoQr: string; fechaModificacion: string }
export interface Voucher { reservationId: number; tour: string; agency: string; start: string; end: string; persons: number; total: number; status: Status; qrPayload: string; checkedInAt: string | null }
export interface Review { id: number; reservaId: number; calificacion: number; comentario: string | null; fechaCreacion: string }
export interface Notification { id: number; reservationId: number; subject: string; message: string; createdAt: string; readAt: string | null }
export interface Page<T> { content: T[]; totalPages: number; number: number; last: boolean }
export interface Video { url: string; durationSeconds: number; format: string }
export interface UploadAuthorization { upload: { uploadUrl: string; apiKey: string; signature: string; parameters: Record<string, string> }; expiresAt: string; maxBytes: number; maxDurationSeconds: number }

import { describe, expect, it } from 'vitest';
import { reservationActions, money } from './format';
import type { Identity, Reservation, Role, Status } from '../types';
const user=(role:Role):Identity=>({role,email:'test@example.com',touristId:1,agencyId:1,guideId:1});
const reservation=(estado:Status)=>({estado,turistaId:1} as Reservation);
describe('acciones por rol y estado',()=>{
  it('un turista puede cancelar, pero no confirmar ni cerrar',()=>{expect(reservationActions(user('TOURIST'),reservation('SCHEDULED'))).toMatchObject({confirm:false,cancel:true,complete:false,noShow:false});});
  it('un turista ajeno no puede cancelar',()=>{expect(reservationActions({...user('TOURIST'),touristId:9},reservation('CONFIRMED')).cancel).toBe(false);});
  it('el operador confirma pero no completa ni marca inasistencia',()=>{expect(reservationActions(user('AGENCY_OPERATOR'),reservation('SCHEDULED')).confirm).toBe(true);expect(reservationActions(user('AGENCY_OPERATOR'),reservation('CONFIRMED'))).toMatchObject({complete:false,noShow:false,checkIn:true});});
  it('el guía completa y canjea, pero no confirma ni marca inasistencia',()=>{expect(reservationActions(user('GUIDE'),reservation('CONFIRMED'))).toMatchObject({complete:true,noShow:false,confirm:false,cancel:false,checkIn:true});});
  it('sólo ADMIN tiene NO_SHOW',()=>{expect(reservationActions(user('ADMIN'),reservation('CONFIRMED')).noShow).toBe(true);expect(reservationActions(user('COORDINATOR'),reservation('CONFIRMED')).noShow).toBe(false);});
  it.each<Status>(['CANCELLED','COMPLETED','NO_SHOW'])('un estado final %s no se reabre',state=>{expect(reservationActions(user('ADMIN'),reservation(state))).toMatchObject({confirm:false,cancel:false,complete:false,noShow:false,checkIn:false});});
  it('la reseña se habilita tras completar y sólo para su titular',()=>{expect(reservationActions(user('TOURIST'),reservation('COMPLETED')).review).toBe(true);expect(reservationActions(user('TOURIST'),reservation('CONFIRMED')).review).toBe(false);});
  it('muestra precios en COP',()=>{expect(money(180000)).toContain('180.000');expect(money(180000)).toContain('COP');});
});

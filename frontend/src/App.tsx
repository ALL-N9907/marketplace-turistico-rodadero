import { lazy, Suspense } from 'react';
import { Link, Route, Routes } from 'react-router-dom';
import { Protected } from './auth';
import { PublicLayout, WorkspaceLayout } from './components/Layout';
import { Loading, Notice, Heading } from './components/ui';
import { Catalog, Compare } from './pages/Catalog';

const Access=lazy(()=>import('./pages/Access').then(m=>({default:m.Access})));
const Agencies=lazy(()=>import('./pages/Agencies').then(m=>({default:m.Agencies})));
const TourDetail=lazy(()=>import('./pages/TourDetail').then(m=>({default:m.TourDetail})));
const Reservations=lazy(()=>import('./pages/Reservations').then(m=>({default:m.Reservations})));
const ReservationDetail=lazy(()=>import('./pages/Reservations').then(m=>({default:m.ReservationDetail})));
const Reviews=lazy(()=>import('./pages/Reviews').then(m=>({default:m.Reviews})));
const Notifications=lazy(()=>import('./pages/Notifications').then(m=>({default:m.Notifications})));
const Dashboard=lazy(()=>import('./pages/Dashboard').then(m=>({default:m.Dashboard})));
const Management=importManagement;
function importManagement<K extends keyof typeof import('./pages/Management')>(name:K){return lazy(()=>import('./pages/Management').then(m=>({default:m[name] as React.ComponentType})));}
const ManageTours=Management('ManageTours'),ManageGuides=Management('ManageGuides'),ManageAgencies=Management('ManageAgencies'),ManageTourists=Management('ManageTourists'),ManageCategories=Management('ManageCategories'),AgencyProfile=Management('AgencyProfile'),LocationPage=Management('LocationPage'),StaffAccounts=Management('StaffAccounts');
const TourContent=lazy(()=>import('./pages/TourManagement').then(m=>({default:m.TourContent})));
const Schedules=lazy(()=>import('./pages/TourManagement').then(m=>({default:m.Schedules})));
const OperationReservations=lazy(()=>import('./pages/Operations').then(m=>({default:m.OperationReservations})));
const CheckIn=lazy(()=>import('./pages/Operations').then(m=>({default:m.CheckIn})));
const Reports=lazy(()=>import('./pages/Reports').then(m=>({default:m.Reports})));
export default function App(){return <Suspense fallback={<div className="container page-space"><Loading/></div>}><Routes><Route element={<PublicLayout/>}><Route index element={<Catalog/>}/><Route path="agencias" element={<Agencies/>}/><Route path="comparar" element={<Compare/>}/><Route path="tours/:id" element={<TourDetail/>}/><Route path="acceso" element={<Access/>}/><Route path="registro" element={<Access key="register" registering/>}/><Route path="pagos" element={<div className="container page-space"><Heading title="Pagos · Próxima etapa">Mercado Pago en entorno de pruebas.</Heading><Notice warning>El checkout aún no está implementado. No ingreses datos de pago ni realices cobros reales.</Notice></div>}/><Route element={<Protected/>}><Route path="notificaciones" element={<Notifications/>}/><Route path="reservas/:id" element={<ReservationDetail/>}/></Route><Route element={<Protected roles={['TOURIST']}/>}><Route path="reservas" element={<Reservations/>}/><Route path="resenas" element={<Reviews/>}/></Route><Route path="*" element={<div className="container empty"><h1>Este camino no existe</h1><Link to="/">Volver a explorar</Link></div>}/></Route><Route element={<Protected roles={['ADMIN','AGENCY_OPERATOR','GUIDE','COORDINATOR']}/>}><Route path="panel" element={<WorkspaceLayout/>}><Route index element={<Dashboard/>}/><Route element={<Protected roles={['ADMIN','AGENCY_OPERATOR']}/>}><Route path="tours" element={<ManageTours/>}/><Route path="tours/:id/contenido" element={<TourContent/>}/><Route path="tours/:id/salidas" element={<Schedules/>}/><Route path="guias" element={<ManageGuides/>}/><Route path="agencias/:id/ubicacion" element={<LocationPage/>}/></Route><Route element={<Protected roles={['ADMIN','AGENCY_OPERATOR','GUIDE']}/>}><Route path="reservas" element={<OperationReservations/>}/><Route path="reservas/:id" element={<ReservationDetail/>}/><Route path="validar" element={<CheckIn/>}/></Route><Route element={<Protected roles={['AGENCY_OPERATOR']}/>}><Route path="perfil" element={<AgencyProfile/>}/></Route><Route element={<Protected roles={['ADMIN','AGENCY_OPERATOR','COORDINATOR']}/>}><Route path="reportes" element={<Reports/>}/></Route><Route element={<Protected roles={['ADMIN']}/>}><Route path="agencias" element={<ManageAgencies/>}/><Route path="turistas" element={<ManageTourists/>}/><Route path="categorias" element={<ManageCategories/>}/><Route path="cuentas" element={<StaffAccounts/>}/></Route></Route></Route></Routes></Suspense>;}

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { useAuth } from '../auth';
import { api, query } from '../lib/api';
import { today } from '../lib/format';
import { Empty, ErrorBox, Field, Heading, Loading, Notice, Section } from '../components/ui';

interface Report { label:string; columns:[string,string][] }
const reports:Record<string,Report>={
  'agency-occupancy':{label:'Ocupación de agencia',columns:[['nombreComercial','Agencia'],['fechaDesde','Desde'],['fechaHasta','Hasta'],['cuposTotales','Cupos totales'],['cuposOcupados','Ocupados'],['porcentajeOcupacion','Ocupación %']]},
  'agency-productivity':{label:'Productividad de agencias',columns:[['posicion','Posición'],['nombreComercial','Agencia'],['toursCompletados','Reservas completadas']]},
  'no-show-tourists':{label:'Turistas con inasistencias',columns:[['nombreCompleto','Turista'],['cantidadNoShow','Inasistencias'],['fechaDesde','Desde'],['fechaHasta','Hasta']]},
  'experience-type-cancellations':{label:'Cancelaciones por experiencia',columns:[['tipoExperiencia','Tipo de experiencia'],['estado','Estado'],['cantidad','Cantidad']]},
};
type Row=Record<string,string|number>;
export function Reports(){
  const {user}=useAuth(),[type,setType]=useState('agency-occupancy'),[search,setSearch]=useState<{type:string;desde:string;hasta:string;agenciaId?:number}|null>(null);
  const result=useQuery({queryKey:['reports',user?.email,search],queryFn:({signal})=>api<Row|Row[]>(`/api/reports/${search!.type}${query({desde:search!.desde,hasta:search!.hasta,agenciaId:search!.type==='agency-occupancy'?search!.agenciaId:undefined})}`,{signal}),enabled:!!search});
  const rows=result.data?(Array.isArray(result.data)?result.data:[result.data]):[];
  const report=search?reports[search.type]:null;
  return <>
    <Heading title="Información para decidir mejor">Reportes operativos reales. Sin ingresos, visitas ni métricas de interés simuladas.</Heading>
    <Section title="Define tu consulta">
      <form onSubmit={e=>{
        e.preventDefault();
        const f=new FormData(e.currentTarget),desde=String(f.get('desde')),hasta=String(f.get('hasta'));
        const end=e.currentTarget.querySelector<HTMLInputElement>('[name=hasta]')!;
        end.setCustomValidity(desde>hasta?'La fecha final debe ser posterior o igual a la inicial.':'');
        if(!end.reportValidity())return;
        setSearch({type,desde,hasta,agenciaId:type==='agency-occupancy'?(user?.agencyId||Number(f.get('agenciaId'))):undefined});
      }}>
        <div className="form-grid">
          <Field label="Reporte"><select value={type} onChange={e=>setType(e.target.value)}>{Object.entries(reports).filter(([k])=>user?.role!=='AGENCY_OPERATOR'||k==='agency-occupancy').map(([key,value])=><option key={key} value={key}>{value.label}</option>)}</select></Field>
          <Field label="Desde"><input type="date" name="desde" defaultValue={`${today().slice(0,7)}-01`} required/></Field>
          <Field label="Hasta"><input type="date" name="hasta" defaultValue={today()} required onChange={e=>e.target.setCustomValidity('')}/></Field>
          {type==='agency-occupancy'&&<Field label="ID de agencia"><input type="number" name="agenciaId" min={1} step={1} defaultValue={user?.agencyId||''} readOnly={!!user?.agencyId} required/></Field>}
        </div>
        <button className="button" disabled={result.isFetching}>Consultar reporte</button>
      </form>
    </Section>
    <ErrorBox error={result.error} retry={()=>void result.refetch()}/>
    {search&&result.isFetching?<Loading/>:report&&result.data&&<Section title={report.label}>
      {rows.length?<>
        <div className="table-wrap"><table><thead><tr>{report.columns.map(([k,label])=><th key={k}>{label}</th>)}</tr></thead><tbody>{rows.map((r,i)=><tr key={i}>{report.columns.map(([k])=><td key={k}>{String(r[k]??'—')}</td>)}</tr>)}</tbody></table></div>
        {search?.type==='agency-productivity'&&<div className="chart" role="img" aria-label="Gráfica de reservas completadas por agencia; datos disponibles en la tabla"><ResponsiveContainer width="100%" height={300}><BarChart data={rows.slice(0,10)}><CartesianGrid strokeDasharray="3 3" vertical={false}/><XAxis dataKey="nombreComercial"/><YAxis allowDecimals={false}/><Tooltip/><Bar dataKey="toursCompletados" name="Reservas completadas" fill="#087f80" radius={[6,6,0,0]}/></BarChart></ResponsiveContainer></div>}
        {search?.type==='agency-occupancy'&&<div className="occupancy-summary"><strong>{rows[0].porcentajeOcupacion}%</strong><div><b>Ocupación en el período</b><p>{rows[0].cuposOcupados} de {rows[0].cuposTotales} cupos · Reservas confirmadas y completadas.</p></div></div>}
      </>:<Empty title="No hay datos para este período"/>}
    </Section>}
    <Notice>La productividad cuenta reservas completadas, no salidas únicas. Los reportes globales sólo están disponibles para ADMIN y COORDINATOR.</Notice>
  </>;
}

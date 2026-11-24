import { useEffect, useRef, type ReactNode, type FormEvent } from 'react';
export function ConfirmDialog({title,children,busy,onCancel,onSubmit}:{title:string;children:ReactNode;busy:boolean;onCancel:()=>void;onSubmit:(e:FormEvent<HTMLFormElement>)=>void}){
  const ref=useRef<HTMLDialogElement>(null);
  useEffect(()=>{ref.current?.showModal();},[]);
  return <dialog ref={ref} className="confirm-dialog" aria-labelledby="dialog-title" onCancel={e=>{e.preventDefault();if(!busy)onCancel();}}><form onSubmit={onSubmit}><h2 id="dialog-title">{title}</h2>{children}<div className="form-actions"><button type="button" className="button secondary" onClick={onCancel} disabled={busy}>Volver</button><button className="button" disabled={busy}>{busy?'Guardando…':'Confirmar acción'}</button></div></form></dialog>;
}

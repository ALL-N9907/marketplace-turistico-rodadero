import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Badge, ErrorBox, Field, Loading } from './ui';
import { ApiError } from '../lib/api';
describe('componentes accesibles',()=>{
  it('el estado no depende sólo del color',()=>{render(<Badge status="SCHEDULED"/>);expect(screen.getByText('Pendiente')).toBeVisible();});
  it('el campo tiene una etiqueta asociada',()=>{render(<Field label="Personas"><input type="number"/></Field>);expect(screen.getByRole('spinbutton',{name:'Personas'})).toBeVisible();});
  it('anuncia conflictos sin mostrar un éxito falso',()=>{render(<ErrorBox error={new ApiError(409,'No quedan cupos.')}/>);expect(screen.getByRole('alert')).toHaveTextContent('No quedan cupos.');});
  it('anuncia la carga',()=>{render(<Loading/>);expect(screen.getByRole('status')).toHaveAccessibleName('Cargando información');});
});

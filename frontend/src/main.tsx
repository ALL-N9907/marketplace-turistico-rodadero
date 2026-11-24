import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './auth';
import { ApiError } from './lib/api';
import App from './App';
import './styles.css';

const client=new QueryClient({defaultOptions:{queries:{staleTime:15_000,retry:(count,error)=>count<1&&(!(error instanceof ApiError)||error.status>=500),refetchOnWindowFocus:false},mutations:{retry:false}}});
ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><QueryClientProvider client={client}><AuthProvider><BrowserRouter><App/></BrowserRouter></AuthProvider></QueryClientProvider></React.StrictMode>);

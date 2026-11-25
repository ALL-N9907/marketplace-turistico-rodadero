import { loadEnv } from 'vite';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    plugins: [react()],
    server: {
      port: 5173,
      // Túnel local de pruebas: autorizar sólo este host, nunca todos los dominios.
      allowedHosts: ['logical-taking-featured-tracked.trycloudflare.com'],
      proxy: { '/api': { target: env.API_PROXY_TARGET || 'http://localhost:8080', changeOrigin: true } },
    },
    test: { environment: 'jsdom', setupFiles: ['./src/test/setup.ts'], include: ['src/**/*.test.{ts,tsx}'], restoreMocks: true },
  };
});

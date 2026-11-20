import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the Spring Boot API runs on :8080; Vite forwards /api to it,
// so the browser sees one origin and no CORS setup is needed.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
});

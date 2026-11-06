import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [react()],
    server: {
        port: 5500,
        // En desarrollo las llamadas a /api pasan por este mismo servidor (sin CORS,
        // aunque Vite use otro puerto porque el 5500 esté ocupado por Docker).
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                configure(proxy) {
                    // Sin el encabezado Origin la API no aplica el filtro CORS (proxy mismo origen)
                    proxy.on('proxyReq', proxyReq => proxyReq.removeHeader('origin'));
                }
            }
        }
    }
});

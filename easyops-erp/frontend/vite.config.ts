import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, __dirname)
  // Where /api is forwarded in dev. Use VITE_DEV_PROXY_TARGET when the browser uses same-origin (empty VITE_API_BASE_URL) to avoid CORS.
  // loadEnv reads .env files; Docker-passed vars land in process.env instead.
  // Check both so `environment:` in docker-compose works without a .env file.
  const apiTarget =
    process.env.VITE_DEV_PROXY_TARGET ||
    env.VITE_DEV_PROXY_TARGET ||
    process.env.VITE_API_BASE_URL ||
    env.VITE_API_BASE_URL ||
    process.env.VITE_AUTH_SERVICE_URL ||
    env.VITE_AUTH_SERVICE_URL ||
    'http://localhost:8081'

  return {
  plugins: [react()],
  resolve: {
    alias: {
      // Package id ends with ".js"; explicit path avoids failed resolution in some Vite/Docker setups.
      // Use package "main" (dist/html2pdf.js), not the optional bundle min file.
      'html2pdf.js': path.resolve(__dirname, 'node_modules/html2pdf.js/dist/html2pdf.js'),
      '@': path.resolve(__dirname, './src'),
      '@components': path.resolve(__dirname, './src/components'),
      '@pages': path.resolve(__dirname, './src/pages'),
      '@services': path.resolve(__dirname, './src/services'),
      '@hooks': path.resolve(__dirname, './src/hooks'),
      '@utils': path.resolve(__dirname, './src/utils'),
      '@types': path.resolve(__dirname, './src/types'),
      '@contexts': path.resolve(__dirname, './src/contexts'),
      '@config': path.resolve(__dirname, './src/config'),
    },
  },
  optimizeDeps: {
    include: ['html2pdf.js'],
  },
  server: {
    port: 3000,
    allowedHosts: [
      'localhost',
      '127.0.0.1',
      'aurora.i2gether.com',
      // Staging & UAT hosts (for local dev:staging / dev:uat)
      '182.160.122.235',
      '172.16.0.45',
      // Production
      'hms.aurora.hospital',
      'www.aurora.hospital',
      'aurora.hospital',
    ],
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true,
      },
    },
  },
  preview: {
    allowedHosts: [
      'localhost',
      '127.0.0.1',
      'aurora.i2gether.com',
      '182.160.122.235',
      '172.16.0.45',
      'hms.aurora.hospital',
      'www.aurora.hospital',
      'aurora.hospital',
    ],
  },
}})
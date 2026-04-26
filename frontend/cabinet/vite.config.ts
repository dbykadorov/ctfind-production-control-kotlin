import { readFileSync } from 'node:fs'
import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

const API_HOST = process.env.CABINET_API_HOST ?? 'http://localhost:8080'

/**
 * 010: Версия SPA из package.json — инжектируется как глобальная константа
 * `__APP_VERSION__` (см. research.md §R-003). Используется в SidebarFooter.vue
 * для отображения "v X.Y.Z" в развёрнутом sidebar (US4 / FR-029..FR-032).
 *
 * Если по какой-то причине package.json не читается (CI без полной checkout),
 * fallback = '0.0.0-dev', чтобы сборка не падала.
 */
function readPackageVersion(): string {
  try {
    const pkgPath = fileURLToPath(new URL('./package.json', import.meta.url))
    const pkg = JSON.parse(readFileSync(pkgPath, 'utf-8')) as { version?: string }
    return pkg.version ?? '0.0.0-dev'
  }
  catch {
    return '0.0.0-dev'
  }
}

const APP_VERSION = readPackageVersion()

export default defineConfig({
  base: '/cabinet/',
  plugins: [vue()],
  define: {
    __APP_VERSION__: JSON.stringify(APP_VERSION),
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    manifest: false,
    sourcemap: true,
    target: 'es2022',
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: API_HOST,
        changeOrigin: true,
        secure: false,
      },
    },
  },
})

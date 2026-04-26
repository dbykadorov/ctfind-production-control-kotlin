/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'

  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>
  export default component
}

interface ImportMetaEnv {
  readonly DEV: boolean
  readonly PROD: boolean
  readonly BASE_URL: string
  readonly MODE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

interface Window {
  __BOOT__?: import('@/api/types/domain').BootPayload
  __CSRF__?: string
}

/**
 * 010: Версия SPA, инжектируется Vite через `define` (см. vite.config.ts §APP_VERSION
 * и research.md §R-003). Доступна как глобальная константа в любом TS/Vue-файле.
 * В runtime-зависимостях vitest она резолвится в `'0.0.0-test'` (см. tests/setup.ts).
 */
declare const __APP_VERSION__: string

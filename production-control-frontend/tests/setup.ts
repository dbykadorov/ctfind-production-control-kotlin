/**
 * Глобальный setup для Vitest. Подменяет boot-payload в `window`, чтобы тесты
 * имели предсказуемого Order Manager без дополнительных моков.
 */

import { afterEach, beforeEach, vi } from 'vitest'

declare global {
  interface Window {
    __BOOT__?: import('@/api/types/domain').BootPayload
    __CSRF__?: string
  }
}

beforeEach(() => {
  window.__BOOT__ = {
    user: 'test@example.com',
    roles: ['Order Manager'],
    language: 'ru',
    csrfToken: 'csrf-test-token',
    siteName: 'test.local',
    cabinetVersion: '0.0.0-test',
  }
  window.__CSRF__ = 'csrf-test-token'
  window.sessionStorage.clear()
  window.localStorage.clear()
})

afterEach(() => {
  vi.restoreAllMocks()
  vi.useRealTimers()
})

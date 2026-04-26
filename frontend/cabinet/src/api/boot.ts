/**
 * Доступ к boot-payload, инжектируемому сервером в `window.__BOOT__` / `window.__CSRF__`.
 * Контракт: specs/006-spa-cabinet-ui/contracts/boot-payload.md.
 *
 * Все остальные модули SPA должны читать boot ТОЛЬКО через эти функции — это позволяет
 * подменять источник в тестах (vitest stubGlobal) и хранить fallback в одном месте.
 *
 * Migration note (002-migrate-cabinet-frontend): fallback Guest payload is the
 * standalone startup path. It lets `/cabinet/login` render without a Frappe
 * page wrapper or boot payload.
 */

import type { BootPayload } from './types/domain'

const FALLBACK_LANGUAGE = 'ru'

function isBootPayload(value: unknown): value is BootPayload {
  if (!value || typeof value !== 'object')
    return false
  const obj = value as Record<string, unknown>
  return (
    typeof obj.user === 'string'
    && Array.isArray(obj.roles)
    && typeof obj.csrfToken === 'string'
    && typeof obj.siteName === 'string'
  )
}

/**
 * Прочитать boot-payload, переданный сервером.
 *
 * Если payload отсутствует или повреждён, возвращает Guest-fallback: пользователь "Guest"
 * без ролей. Это безопасно: router-guard перенаправит на `/cabinet/login`, а сервер при
 * любом API-вызове ответит 403/401.
 */
export function readBoot(): BootPayload {
  const raw = typeof window !== 'undefined' ? window.__BOOT__ : undefined
  if (isBootPayload(raw)) {
    return {
      ...raw,
      language: raw.language || FALLBACK_LANGUAGE,
    }
  }
  return {
    user: 'Guest',
    roles: [],
    language: FALLBACK_LANGUAGE,
    csrfToken: '',
    siteName: '',
    deskUrl: '/app',
    cabinetVersion: '0.0.0',
  }
}

/** Прочитать актуальный CSRF-токен (после refresh может отличаться от boot.csrfToken). */
export function readCsrfToken(): string {
  if (typeof window === 'undefined')
    return ''
  return window.__CSRF__ || window.__BOOT__?.csrfToken || ''
}

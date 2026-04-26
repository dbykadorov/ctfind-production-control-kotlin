/**
 * Auth-сервис Кабинета.
 *
 * В feature-slice `002-migrate-cabinet-frontend` новая backend-авторизация ещё
 * не подключена. Форма входа остается интерактивной, но не вызывает старый
 * Frappe login endpoint и всегда возвращает понятный placeholder outcome.
 */

import { frappeCall } from './frappe-client'

/**
 * Текстовые ключи для i18n-вывода ошибок на форме. Каждый ключ соответствует
 * одной строке в `i18n/keys.ts → login.error.*` и `i18n/ru.ts`.
 */
export type LoginErrorKey =
  | 'invalid' // неверный логин ИЛИ неверный пароль (одно сообщение, see SC-004)
  | 'disabled' // user.enabled = 0
  | 'rateLimit' // 429 / 417 от Frappe
  | 'twoFa' // user has 2FA enabled — направляем в Frappe Desk (см. spec Q3=C)
  | 'network' // 5xx или сетевая ошибка
  | 'empty' // пустые поля (validation на клиенте)
  | 'unavailable' // новая platform auth-интеграция пока не подключена

/**
 * Дискриминированное объединение результата вызова `loginViaCabinet`.
 *
 * - `success` — оставлен в типе для будущей auth-интеграции, но не возвращается
 *   в текущем feature-slice.
 * - `error` — credential / disabled / rate-limit / network. Caller показывает
 *   inline-сообщение `login.error.<messageKey>`.
 * - `two-fa-required` — Frappe вернул `verification` payload. Caller должен
 *   показать сообщение `login.error.twoFa` со ссылкой на стандартную Frappe-форму.
 */
export type LoginOutcome =
  | { kind: 'success' }
  | { kind: 'error', messageKey: LoginErrorKey }
  | { kind: 'two-fa-required' }

/**
 * Выполнить логин через форму Кабинета.
 *
 * @param usr Login (email или username, как требует Frappe).
 * @param pwd Plaintext пароль.
 */
export async function loginViaCabinet(usr: string, pwd: string): Promise<LoginOutcome> {
  if (!usr || !pwd)
    return { kind: 'error', messageKey: 'empty' }

  return { kind: 'error', messageKey: 'unavailable' }
}

/**
 * Выполнить logout через стандартный Frappe endpoint. Не делает редирект —
 * caller (TopBar.vue) сам вызовет `window.location.assign('/cabinet/login')`
 * (см. research.md §R-008).
 */
export async function logoutFromCabinet(): Promise<void> {
  try {
    await frappeCall('logout', {}, { method: 'POST' })
  }
  catch {
    // logout всё равно должен выполниться — даже при сетевой ошибке мы
    // дальше делаем full-page reload, который сбросит client state.
  }
}

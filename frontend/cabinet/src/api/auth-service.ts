/**
 * Auth-сервис Кабинета.
 */

import { httpClient } from './api-client'

export const AUTH_API_BASE_URL = '/api/auth'
export { AUTH_TOKEN_STORAGE_KEY } from './api-client'

/**
 * Текстовые ключи для i18n-вывода ошибок на форме. Каждый ключ соответствует
 * одной строке в `i18n/keys.ts → login.error.*` и `i18n/ru.ts`.
 */
export type LoginErrorKey =
  | 'invalid' // неверный логин ИЛИ неверный пароль (одно сообщение, see SC-004)
  | 'disabled' // user.enabled = 0
  | 'rateLimit' // 429
  | 'twoFa' // reserved for future auth policies
  | 'network' // 5xx или сетевая ошибка
  | 'empty' // пустые поля (validation на клиенте)
  | 'unavailable' // auth API временно недоступен

/**
 * Дискриминированное объединение результата вызова `loginViaCabinet`.
 *
 * - `error` — credential / disabled / rate-limit / network. Caller показывает
 *   inline-сообщение `login.error.<messageKey>`.
 * - `two-fa-required` — reserved branch for a future second-factor flow.
 */
export type LoginOutcome =
  | ({ kind: 'success' } & LoginSuccessPayload)
  | { kind: 'error', messageKey: LoginErrorKey }
  | { kind: 'two-fa-required' }

export interface AuthenticatedUserPayload {
  login: string
  displayName: string
  roles: string[]
}

export interface LoginSuccessPayload {
  tokenType: 'Bearer'
  accessToken: string
  expiresAt: string
  user: AuthenticatedUserPayload
}

export interface AuthenticatedUserSession extends AuthenticatedUserPayload {
  expiresAt: string
}

/**
 * Выполнить логин через форму Кабинета.
 *
 * @param usr Login (email или username).
 * @param pwd Plaintext пароль.
 */
export async function loginViaCabinet(usr: string, pwd: string): Promise<LoginOutcome> {
  if (!usr || !pwd)
    return { kind: 'error', messageKey: 'empty' }

  try {
    const response = await httpClient.post<LoginSuccessPayload>(`${AUTH_API_BASE_URL}/login`, {
      login: usr,
      password: pwd,
    })

    return {
      kind: 'success',
      ...response.data,
    }
  }
  catch (error) {
    const status = (error as { response?: { status?: number } }).response?.status
    if (status === 401)
      return { kind: 'error', messageKey: 'invalid' }
    if (status === 429)
      return { kind: 'error', messageKey: 'rateLimit' }
    if (status === 400)
      return { kind: 'error', messageKey: 'empty' }
    return { kind: 'error', messageKey: 'network' }
  }
}

export async function fetchAuthenticatedUser(token: string): Promise<AuthenticatedUserSession> {
  const response = await httpClient.get<AuthenticatedUserSession>(`${AUTH_API_BASE_URL}/me`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })
  return response.data
}

/**
 * Выполнить logout. Не делает редирект —
 * caller (TopBar.vue) сам вызовет `window.location.assign('/cabinet/login')`
 * (см. research.md §R-008).
 */
export async function logoutFromCabinet(): Promise<void> {
  try {
    await httpClient.post(`${AUTH_API_BASE_URL}/logout`, {})
  }
  catch {
    // logout всё равно должен выполниться — даже при сетевой ошибке мы
    // дальше делаем full-page reload, который сбросит client state.
  }
}

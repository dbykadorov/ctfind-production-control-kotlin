/**
 * HTTP-клиент Кабинета поверх axios. Подмешивает CSRF, обрабатывает 401, делает 1 ретрай
 * для GET (см. R-007 в research.md). Все вызовы Frappe `/api/method/...` идут через
 * `frappeCall(method, params)`.
 *
 * Контракты: specs/006-spa-cabinet-ui/contracts/http-endpoints.md.
 *
 * Migration note (002-migrate-cabinet-frontend): этот модуль оставлен как
 * legacy integration boundary. Он не делает HTTP-запросов при import; в Docker
 * local runtime Vite proxy направляет `/api` в новый Spring/Kotlin backend, а не
 * в старый Frappe runtime.
 */

import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import type { ApiError } from './types/domain'
import axios from 'axios'
import { toApiError } from '@/utils/errors'
import { sanitizeFrom } from '@/utils/url'
import { readCsrfToken } from './boot'

type SessionExpiredHandler = (err: ApiError) => void
const AUTH_TOKEN_STORAGE_KEY = 'ctfind.cabinet.authToken'

let sessionExpiredHandler: SessionExpiredHandler | null = null

/**
 * 009-cabinet-custom-login: при 401 от защищённого endpoint'а делаем full-page
 * redirect на `/cabinet/login?from=<current>&reason=session-expired` (FR-009).
 * Эта переменная позволяет тестам подменить `window.location.assign`
 * (по умолчанию — настоящий браузерный navigate).
 */
let sessionExpiredRedirectInProgress = false

/** Установить глобальный обработчик 401 (вызывается из stores/auth.ts при инициализации). */
export function onSessionExpired(handler: SessionExpiredHandler): void {
  sessionExpiredHandler = handler
}

interface RetryConfig extends InternalAxiosRequestConfig {
  __retried?: boolean
}

const RETRYABLE_STATUSES = new Set([502, 503, 504])

function shouldRetry(config: RetryConfig | undefined, status: number | undefined): boolean {
  if (!config || config.__retried)
    return false
  const method = (config.method || 'get').toLowerCase()
  if (method !== 'get')
    return false
  if (status === undefined)
    return true
  return RETRYABLE_STATUSES.has(status)
}

function createClient(): AxiosInstance {
  const instance = axios.create({
    baseURL: '/',
    withCredentials: true,
    headers: {
      'X-Frappe-Site-Name': '',
      'Accept': 'application/json',
    },
    timeout: 30_000,
    paramsSerializer: {
      serialize: params => serializeForFrappe(params).toString(),
    },
  })

  instance.interceptors.request.use((config) => {
    const csrf = readCsrfToken()
    if (csrf) {
      config.headers = config.headers ?? {}
      config.headers.set?.('X-Frappe-CSRF-Token', csrf)
    }
    config.headers = config.headers ?? {}
    applyBearerToken(config.headers)
    return config
  })

  instance.interceptors.response.use(
    response => response,
    async (error) => {
      const apiErr = toApiError(error)
      const originalConfig = error.config as RetryConfig | undefined

      if (shouldRetry(originalConfig, apiErr.status)) {
        originalConfig!.__retried = true
        await new Promise(resolve => setTimeout(resolve, 250))
        return instance.request(originalConfig!)
      }

      if (apiErr.kind === 'session-expired') {
        if (sessionExpiredHandler)
          sessionExpiredHandler(apiErr)
        triggerSessionExpiredRedirect()
      }

      return Promise.reject(apiErr)
    },
  )

  return instance
}

/**
 * Полный full-page redirect на `/cabinet/login?from=<current>&reason=session-expired`.
 *
 * Идемпотентен: если уже инициирован — повторные 401 (например, от параллельных
 * запросов на одной странице) не плодят редиректов. Если мы УЖЕ на /cabinet/login —
 * редирект не нужен (форма сама покажет уведомление по ?reason=).
 */
function triggerSessionExpiredRedirect(): void {
  if (sessionExpiredRedirectInProgress)
    return
  if (typeof window === 'undefined')
    return

  const currentPath = `${window.location.pathname}${window.location.search}`
  if (window.location.pathname.startsWith('/cabinet/login'))
    return

  sessionExpiredRedirectInProgress = true
  const from = sanitizeFrom(currentPath) ?? '/cabinet'
  const url = `/cabinet/login?from=${encodeURIComponent(from)}&reason=session-expired`
  window.location.assign(url)
}

export const httpClient: AxiosInstance = createClient()

export function applyBearerToken(headers: { set?: (key: string, value: string) => unknown, Authorization?: unknown }): void {
  if (typeof window === 'undefined')
    return
  const token = window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
  if (!token)
    return
  if (typeof headers.set === 'function') {
    headers.set('Authorization', `Bearer ${token}`)
    return
  }
  headers.Authorization = `Bearer ${token}`
}

interface FrappeMethodResponse<T> {
  message: T
  exc?: string
  exc_type?: string
}

export interface FrappeCallOptions {
  /** Метод HTTP. По умолчанию POST для совместимости с Frappe (включая ?cmd=...). */
  method?: 'GET' | 'POST'
  signal?: AbortSignal
  /** Не подмешивать CSRF (нужно для публичных методов вроде `login`). */
  withoutCsrf?: boolean
  /**
   * Кастомные заголовки. Подмешиваются в HTTP-запрос ПОСЛЕ CSRF — то есть могут
   * перезаписывать дефолтные значения (включая `Content-Type` для POST,
   * если явно указано). Используется фичей 009 для проброса `X-Cabinet-Login: 1`
   * (см. specs/009-cabinet-custom-login/contracts/cabinet-login-contract.md §C-1).
   */
  headers?: Record<string, string>
}

/**
 * Вызвать Frappe-метод по dotted-path, например `frappe.client.get_list`.
 * Возвращает значение поля `message` из ответа Frappe.
 */
export async function frappeCall<T = unknown>(
  method: string,
  params: Record<string, unknown> = {},
  options: FrappeCallOptions = {},
): Promise<T> {
  const httpMethod = options.method ?? 'POST'
  const url = `/api/method/${method}`

  const config: AxiosRequestConfig = {
    headers: { ...(options.headers ?? {}) },
    signal: options.signal,
  }

  if (options.withoutCsrf) {
    delete (config.headers as Record<string, string>)['X-Frappe-CSRF-Token']
  }

  let response: AxiosResponse<FrappeMethodResponse<T>>
  if (httpMethod === 'GET') {
    response = await httpClient.get(url, { ...config, params })
  }
  else {
    const body = serializeForFrappe(params)
    response = await httpClient.post(url, body, {
      ...config,
      headers: {
        ...config.headers,
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    })
  }
  return response.data.message
}

/**
 * Frappe ожидает form-encoded payload, в котором сложные значения (object, array)
 * сериализованы как JSON-строки. Это требование `frappe.handler.handle()`.
 *
 * Используется и для тела POST, и (через `paramsSerializer` axios) для query-string GET,
 * чтобы массивы вида `[["status","=","Active"]]` не превращались в bracket-notation
 * (`filters[0][0]=status...`), которую Frappe парсит как kwarg `filters[0][0]` и роняет
 * `DatabaseQuery.execute()` с `unexpected keyword argument`.
 */
export function serializeForFrappe(params: Record<string, unknown>): URLSearchParams {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null)
      continue
    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
      search.append(key, String(value))
    }
    else {
      search.append(key, JSON.stringify(value))
    }
  }
  return search
}

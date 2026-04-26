import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import type { ApiError } from './types/domain'
import axios from 'axios'
import { toApiError } from '@/utils/errors'
import { sanitizeFrom } from '@/utils/url'

type SessionExpiredHandler = (err: ApiError) => void

export const AUTH_TOKEN_STORAGE_KEY = 'ctfind.cabinet.authToken'

let sessionExpiredHandler: SessionExpiredHandler | null = null
let sessionExpiredRedirectInProgress = false

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
    headers: {
      Accept: 'application/json',
    },
    timeout: 30_000,
  })

  instance.interceptors.request.use((config) => {
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
        sessionExpiredHandler?.(apiErr)
        triggerSessionExpiredRedirect()
      }

      return Promise.reject(apiErr)
    },
  )

  return instance
}

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

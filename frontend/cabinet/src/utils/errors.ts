/**
 * Маппинг сырых HTTP-ошибок Frappe в типизированные `ApiError`-варианты.
 * См. specs/006-spa-cabinet-ui/contracts/http-endpoints.md §Error Shape.
 */

import type { AxiosError } from 'axios'
import type { ApiError, ApiErrorKind } from '@/api/types/domain'

interface FrappeErrorPayload {
  exc_type?: string
  exception?: string
  exc?: string
  _server_messages?: string
  message?: string
  messages?: string[]
}

const EXC_TYPE_TO_KIND: Record<string, ApiErrorKind> = {
  TimestampMismatchError: 'conflict',
  PermissionError: 'permission',
  ValidationError: 'validation',
  MandatoryError: 'validation',
  AuthenticationError: 'session-expired',
  CSRFTokenError: 'session-expired',
  SessionExpired: 'session-expired',
}

function safeParseServerMessages(raw: string | undefined): string[] {
  if (!raw)
    return []
  try {
    const parsed = JSON.parse(raw) as unknown
    const list = Array.isArray(parsed) ? parsed : [parsed]
    return list.map((entry) => {
      if (typeof entry === 'string') {
        try {
          const obj = JSON.parse(entry) as { message?: string }
          return obj.message ?? entry
        }
        catch {
          return entry
        }
      }
      const e = entry as { message?: string }
      return e?.message ?? String(entry)
    })
  }
  catch {
    return [raw]
  }
}

function detectKind(status: number | undefined, payload: FrappeErrorPayload): ApiErrorKind {
  if (payload.exc_type && EXC_TYPE_TO_KIND[payload.exc_type]) {
    return EXC_TYPE_TO_KIND[payload.exc_type]!
  }
  if (status === 401 || status === 403) {
    return status === 401 ? 'session-expired' : 'permission'
  }
  if (status === 409)
    return 'conflict'
  if (status && status >= 500)
    return 'server'
  if (status && status >= 400)
    return 'validation'
  return 'unknown'
}

function pickMessage(payload: FrappeErrorPayload): string {
  if (payload.message && typeof payload.message === 'string')
    return payload.message
  const fromServer = safeParseServerMessages(payload._server_messages)
  if (fromServer.length > 0 && fromServer[0])
    return fromServer[0]
  if (payload.messages && payload.messages.length > 0 && payload.messages[0])
    return payload.messages[0]
  return 'Произошла ошибка при обращении к серверу.'
}

/** Преобразовать AxiosError в типизированную `ApiError`. */
export function toApiError(error: unknown): ApiError {
  const axiosErr = error as AxiosError<FrappeErrorPayload>
  if (axiosErr?.isAxiosError) {
    if (!axiosErr.response) {
      return {
        kind: 'network',
        message: 'Не удалось связаться с сервером. Проверьте соединение.',
        raw: axiosErr.message,
      }
    }
    const payload: FrappeErrorPayload = (axiosErr.response.data ?? {}) as FrappeErrorPayload
    const kind = detectKind(axiosErr.response.status, payload)
    return {
      kind,
      message: pickMessage(payload),
      excType: payload.exc_type,
      status: axiosErr.response.status,
      raw: axiosErr.response.data,
    }
  }

  if (error instanceof Error) {
    return { kind: 'unknown', message: error.message, raw: error }
  }

  return { kind: 'unknown', message: 'Неизвестная ошибка', raw: error }
}

export function isConflict(error: ApiError): boolean {
  return error.kind === 'conflict'
}

export function isSessionExpired(error: ApiError): boolean {
  return error.kind === 'session-expired'
}

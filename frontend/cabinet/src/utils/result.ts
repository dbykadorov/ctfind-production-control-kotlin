/**
 * Result<T, E> — типобезопасная обёртка для composables, чтобы потребитель различал
 * успех/ошибку через discriminator `ok`. Используется в `frappe-client.ts` и
 * composables, не пробрасывающих исключения наружу.
 */

import type { ApiError } from '@/api/types/domain'

export interface Ok<T> {
  ok: true
  value: T
}

export interface Err<E = ApiError> {
  ok: false
  error: E
}

export type Result<T, E = ApiError> = Ok<T> | Err<E>

export function ok<T>(value: T): Ok<T> {
  return { ok: true, value }
}

export function err<E = ApiError>(error: E): Err<E> {
  return { ok: false, error }
}

export function isOk<T, E>(result: Result<T, E>): result is Ok<T> {
  return result.ok
}

export function isErr<T, E>(result: Result<T, E>): result is Err<E> {
  return !result.ok
}

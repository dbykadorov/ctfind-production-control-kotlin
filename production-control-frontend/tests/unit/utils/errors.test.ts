import { describe, expect, it } from 'vitest'
import type { ApiError } from '@/api/types/domain'
import { isAbortLikeError, toApiError } from '@/utils/errors'

describe('errors / isAbortLikeError', () => {
  it('recognizes Axios cancel code', () => {
    expect(isAbortLikeError({ code: 'ERR_CANCELED' })).toBe(true)
  })
})

describe('errors / toApiError idempotence', () => {
  it('does not double-wrap ApiError from interceptor', () => {
    const already: ApiError = {
      kind: 'network',
      message: 'Не удалось связаться с сервером. Проверьте соединение.',
      raw: 'x',
    }
    expect(toApiError(already)).toBe(already)
  })
})

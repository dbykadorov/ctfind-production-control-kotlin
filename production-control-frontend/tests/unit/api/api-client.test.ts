import { describe, expect, it } from 'vitest'
import { applyBearerToken, AUTH_TOKEN_STORAGE_KEY } from '@/api/api-client'

describe('applyBearerToken', () => {
  it('adds Authorization from localStorage', () => {
    window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, 'jwt-admin')
    const headers = new Headers()

    applyBearerToken(headers)

    expect(headers.get('Authorization')).toBe('Bearer jwt-admin')
  })
})

/**
 * Unit-тесты sanitizeFrom (009-cabinet-custom-login, T009).
 *
 * Тестирует open-redirect защиту: только `/cabinet` и `/cabinet/...` пропускаются;
 * всё остальное (включая protocol-relative, absolute URL, бэкслеши) → null.
 */

import { describe, expect, it } from 'vitest'
import { sanitizeFrom } from '@/utils/url'

describe('sanitizeFrom', () => {
  it('пропускает /cabinet ровно', () => {
    expect(sanitizeFrom('/cabinet')).toBe('/cabinet')
  })

  it('пропускает любой /cabinet/* путь', () => {
    expect(sanitizeFrom('/cabinet/orders')).toBe('/cabinet/orders')
    expect(sanitizeFrom('/cabinet/orders/new')).toBe('/cabinet/orders/new')
    expect(sanitizeFrom('/cabinet/customers?search=foo')).toBe('/cabinet/customers?search=foo')
    expect(sanitizeFrom('/cabinet/orders/AAA-001#history')).toBe('/cabinet/orders/AAA-001#history')
  })

  it('отвергает protocol-relative URL', () => {
    expect(sanitizeFrom('//evil.example.com/cabinet')).toBeNull()
    expect(sanitizeFrom('//cabinet')).toBeNull()
  })

  it('отвергает absolute URL', () => {
    expect(sanitizeFrom('http://evil.example.com/cabinet')).toBeNull()
    expect(sanitizeFrom('https://evil.example.com/cabinet/orders')).toBeNull()
    expect(sanitizeFrom('javascript:alert(1)')).toBeNull()
  })

  it('отвергает бэкслеши (защита от Windows path tricks)', () => {
    expect(sanitizeFrom('/cabinet\\..\\app')).toBeNull()
    expect(sanitizeFrom('\\\\evil')).toBeNull()
  })

  it('отвергает не-/cabinet пути', () => {
    expect(sanitizeFrom('/app')).toBeNull()
    expect(sanitizeFrom('/login')).toBeNull()
    expect(sanitizeFrom('/api/method/login')).toBeNull()
    expect(sanitizeFrom('/')).toBeNull()
    expect(sanitizeFrom('cabinet')).toBeNull()
  })

  it('отвергает не-string и пустые значения', () => {
    expect(sanitizeFrom(undefined)).toBeNull()
    expect(sanitizeFrom(null)).toBeNull()
    expect(sanitizeFrom('')).toBeNull()
    expect(sanitizeFrom(123)).toBeNull()
    expect(sanitizeFrom({ from: '/cabinet' })).toBeNull()
    expect(sanitizeFrom(['/cabinet'])).toBeNull()
  })

  it('пропускает /cabinet/ префикс с trailing slash', () => {
    expect(sanitizeFrom('/cabinet/')).toBe('/cabinet/')
  })
})

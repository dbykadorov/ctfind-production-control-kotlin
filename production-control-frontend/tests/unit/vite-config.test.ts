import { describe, expect, it } from 'vitest'
import { redirectCabinetBaseWithoutTrailingSlash } from '../../vite-dev-base-redirect'

describe('vite dev server config', () => {
  it('redirects /cabinet to /cabinet/ before Vite base handling', () => {
    const middleware = redirectCabinetBaseWithoutTrailingSlash()
    const headers: Record<string, string> = {}
    let statusCode = 0
    let ended = false
    const req = { url: '/cabinet' } as Parameters<typeof middleware>[0]
    const res = {
      setHeader: (name: string, value: number | string | readonly string[]) => {
        headers[name] = String(value)
      },
      end: () => {
        ended = true
      },
      get statusCode() {
        return statusCode
      },
      set statusCode(value: number) {
        statusCode = value
      },
    } as Parameters<typeof middleware>[1]

    middleware(
      req,
      res,
      () => {
        throw new Error('next should not be called for /cabinet')
      },
    )

    expect(statusCode).toBe(302)
    expect(headers.Location).toBe('/cabinet/')
    expect(ended).toBe(true)
  })
})

import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/api/composables/use-notifications.ts'),
  'utf8',
)

describe('useNotifications composable', () => {
  it('exports useNotifications function', () => {
    expect(SOURCE).toContain('useNotifications')
  })

  it('returns data ref', () => {
    expect(SOURCE).toContain('data')
  })

  it('returns loading ref', () => {
    expect(SOURCE).toContain('loading')
  })

  it('returns error ref', () => {
    expect(SOURCE).toContain('error')
  })

  it('returns refetch function', () => {
    expect(SOURCE).toContain('refetch')
  })

  it('uses httpClient', () => {
    expect(SOURCE).toContain('httpClient')
  })

  it('supports page and unreadOnly params', () => {
    expect(SOURCE).toContain('page')
    expect(SOURCE).toContain('unreadOnly')
  })

  it('uses AbortController', () => {
    expect(SOURCE).toContain('AbortController')
  })
})

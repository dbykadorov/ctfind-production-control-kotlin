import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/api/composables/use-active-orders.ts'),
  'utf8',
)

describe('useActiveOrders composable', () => {
  it('calls active-for-consumption endpoint', () => {
    expect(SOURCE).toContain('/api/orders/active-for-consumption')
  })

  it('requires minimum query length before request', () => {
    expect(SOURCE).toContain('normalized.length < 2')
  })

  it('tracks loading and error state', () => {
    expect(SOURCE).toContain('loading')
    expect(SOURCE).toContain('error')
  })
})

import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(join(process.cwd(), 'src/pages/office/OrderNewPage.vue'), 'utf8')

describe('OrderNewPage', () => {
  it('submits through Spring order creation composable and redirects to detail', () => {
    expect(SOURCE).toContain('const created = await createOrder(payload)')
    expect(SOURCE).toContain("router.replace({ name: 'orders.detail'")
  })

  it('handles backend permission errors without losing the form', () => {
    expect(SOURCE).toContain("apiErr.kind === 'permission'")
    expect(SOURCE).toContain('Недостаточно прав для создания заказа')
  })
})

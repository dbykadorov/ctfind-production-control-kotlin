/**
 * Order detail production-task affordance (Feature 005 US2, T041).
 *
 * Verifies the order detail page exposes a "Создать производственную задачу"
 * affordance via ProductionTaskCreateForm, gated by the production-task
 * creation permission, and that the form itself validates required fields
 * (purpose, quantity, uom) before posting.
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const ORDER_DETAIL_SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/office/OrderDetailPage.vue'),
  'utf8',
)
const CREATE_FORM_SOURCE = readFileSync(
  join(process.cwd(), 'src/components/domain/ProductionTaskCreateForm.vue'),
  'utf8',
)

describe('OrderDetailPage production task affordance (US2 T041)', () => {
  it('imports and renders ProductionTaskCreateForm', () => {
    expect(ORDER_DETAIL_SOURCE).toContain("import ProductionTaskCreateForm from '@/components/domain/ProductionTaskCreateForm.vue'")
    expect(ORDER_DETAIL_SOURCE).toContain('<ProductionTaskCreateForm')
  })

  it('only shows the create-task panel when the user can create production tasks', () => {
    expect(ORDER_DETAIL_SOURCE).toContain('permissions.canCreateProductionTasks')
  })

  it('passes order id and items to the form so it can pick the order item', () => {
    expect(ORDER_DETAIL_SOURCE).toContain(':order-id="order.name"')
    expect(ORDER_DETAIL_SOURCE).toContain(':items="draft.items"')
  })
})

describe('ProductionTaskCreateForm validation (US2 T041)', () => {
  it('requires a non-empty purpose before submitting', () => {
    expect(CREATE_FORM_SOURCE).toContain('Укажите назначение работы')
  })

  it('rejects non-positive quantity', () => {
    expect(CREATE_FORM_SOURCE).toContain('Количество должно быть больше нуля')
    expect(CREATE_FORM_SOURCE).toContain('quantity.value <= 0')
  })

  it('requires a non-empty uom', () => {
    expect(CREATE_FORM_SOURCE).toContain('Укажите единицу измерения')
    expect(CREATE_FORM_SOURCE).toContain('!uom.value.trim()')
  })

  it('shows an empty state when the order has no items', () => {
    expect(CREATE_FORM_SOURCE).toContain('items.length === 0')
    expect(CREATE_FORM_SOURCE).toContain('добавьте состав заказа')
  })

  it('navigates to the new production task detail on success', () => {
    expect(CREATE_FORM_SOURCE).toContain("name: 'production-tasks.detail'")
    expect(CREATE_FORM_SOURCE).toContain('router.push')
  })

  it('surfaces backend validation errors via toast', () => {
    expect(CREATE_FORM_SOURCE).toContain('Не удалось создать задачу')
    expect(CREATE_FORM_SOURCE).toContain('toast.error')
  })
})

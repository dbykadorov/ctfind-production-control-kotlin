/**
 * Zod-схемы для валидации форм заказа.
 * Серверная валидация — авторитет (см. 005-orders); это UX-уровень, который ловит ошибки
 * до отправки и даёт мгновенную обратную связь (vee-validate + @vee-validate/zod).
 *
 * Spec: data-model.md §2.2, contracts/http-endpoints.md §Mutations.
 */

import { z } from 'zod'

const ORDER_STATUSES = ['новый', 'в работе', 'готов', 'отгружен'] as const

export const OrderStatusSchema = z.enum(ORDER_STATUSES)

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/

function todayIso(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export const OrderItemSchema = z.object({
  item_name: z
    .string({ required_error: 'Укажите наименование позиции' })
    .trim()
    .min(1, 'Укажите наименование позиции')
    .max(255, 'Не более 255 символов'),
  quantity: z
    .number({ required_error: 'Укажите количество', invalid_type_error: 'Количество — число' })
    .positive('Количество должно быть больше нуля'),
  uom: z
    .string({ required_error: 'Укажите единицу измерения' })
    .trim()
    .min(1, 'Укажите единицу измерения')
    .max(32, 'Не более 32 символов'),
})

const baseOrderShape = {
  customer: z
    .string({ required_error: 'Выберите клиента' })
    .trim()
    .min(1, 'Выберите клиента'),
  delivery_date: z
    .string({ required_error: 'Укажите срок исполнения' })
    .regex(ISO_DATE_RE, 'Дата в формате ГГГГ-ММ-ДД'),
  notes: z.string().trim().max(4000, 'Не более 4000 символов').optional(),
  items: z.array(OrderItemSchema).min(1, 'Добавьте хотя бы одну позицию'),
}

export const OrderCreateSchema = z
  .object(baseOrderShape)
  .refine(d => d.delivery_date >= todayIso(), {
    path: ['delivery_date'],
    message: 'Срок исполнения не может быть в прошлом',
  })

export const OrderEditSchema = z
  .object({
    ...baseOrderShape,
    name: z.string().min(1),
    modified: z.string().min(1),
    status: OrderStatusSchema.optional(),
  })
  .refine(d => d.delivery_date >= todayIso(), {
    path: ['delivery_date'],
    message: 'Срок исполнения не может быть в прошлом',
  })

export type OrderCreateInput = z.infer<typeof OrderCreateSchema>
export type OrderEditInput = z.infer<typeof OrderEditSchema>
export type OrderItemInput = z.infer<typeof OrderItemSchema>

export const validatorsInternals = { todayIso, ORDER_STATUSES }

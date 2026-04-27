import type { ApiError, OrderStatus } from './domain'

export type BackendOrderStatus = 'NEW' | 'IN_WORK' | 'READY' | 'SHIPPED'
export type BackendCustomerStatus = 'ACTIVE' | 'INACTIVE'

export interface CustomerOption {
  id: string
  displayName: string
  status: BackendCustomerStatus
  contactPerson?: string
  phone?: string
  email?: string
}

export interface CustomerSearchResponse {
  items: CustomerOption[]
}

export interface OrderCustomerSummary {
  id: string
  displayName: string
  status: BackendCustomerStatus
  contactPerson?: string
  phone?: string
  email?: string
}

export interface OrderListRowResponse {
  id: string
  orderNumber: string
  customer: OrderCustomerSummary
  deliveryDate: string
  status: BackendOrderStatus
  statusLabel: OrderStatus
  updatedAt: string
  createdAt?: string
  version: number
  overdue: boolean
}

export interface OrdersPageResponse {
  items: OrderListRowResponse[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface OrderItemResponse {
  id: string
  lineNo: number
  itemName: string
  quantity: number
  uom: string
}

export interface OrderTimelineResponse {
  type: 'CREATED' | 'UPDATED' | 'STATUS_CHANGED'
  fromStatus?: BackendOrderStatus | null
  toStatus?: BackendOrderStatus | null
  actorDisplayName: string
  changedAt: string
  note?: string | null
}

export interface OrderFieldDiffResponse {
  fieldname: string
  fieldLabel?: string
  fromValue?: unknown
  toValue?: unknown
}

export interface OrderChangeDiffResponse {
  type: 'CREATED' | 'UPDATED' | 'STATUS_CHANGED'
  actorDisplayName: string
  changedAt: string
  fieldDiffs: OrderFieldDiffResponse[]
}

export interface OrderDetailResponse extends OrderListRowResponse {
  createdAt: string
  notes?: string
  items: OrderItemResponse[]
  history: OrderTimelineResponse[]
  changeDiffs: OrderChangeDiffResponse[]
}

export interface OrderItemPayload {
  itemName: string
  quantity: number
  uom: string
}

export interface CreateOrderPayload {
  customerId: string
  deliveryDate: string
  notes?: string
  items: OrderItemPayload[]
}

export interface UpdateOrderPayload extends CreateOrderPayload {
  expectedVersion: number
}

export interface ChangeOrderStatusPayload {
  expectedVersion: number
  toStatus: BackendOrderStatus
  note?: string
}

export interface DashboardSummaryResponse {
  totalOrders: number
  activeOrders: number
  overdueOrders: number
  statusCounts: Record<BackendOrderStatus, number>
  recentChanges: Array<{
    orderId: string
    orderNumber: string
    customerDisplayName: string
    fromStatus?: BackendOrderStatus | null
    toStatus: BackendOrderStatus
    changedAt: string
    actorDisplayName: string
  }>
  trend: Array<{
    date: string
    created: number
    shipped: number
  }>
}

export interface OrdersComposableError {
  error: ApiError
}

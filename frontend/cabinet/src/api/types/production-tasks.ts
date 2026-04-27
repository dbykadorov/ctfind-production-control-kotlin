export type ProductionTaskStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'BLOCKED' | 'COMPLETED'

export type ProductionTaskAction = 'ASSIGN' | 'PLAN' | 'START' | 'BLOCK' | 'UNBLOCK' | 'COMPLETE'

export interface ProductionTaskOrderSummary {
  id: string
  orderNumber: string
  customerDisplayName: string
  status?: string
  deliveryDate?: string
}

export interface ProductionTaskOrderItemSummary {
  id: string
  lineNo: number
  itemName: string
  quantity?: number
  uom?: string
}

export interface ProductionTaskExecutorSummary {
  id: string
  displayName: string
  login?: string
}

export interface ProductionTaskListRowResponse {
  id: string
  taskNumber: string
  purpose: string
  order: ProductionTaskOrderSummary
  orderItem?: ProductionTaskOrderItemSummary
  quantity: number
  uom: string
  status: ProductionTaskStatus
  statusLabel: string
  executor?: ProductionTaskExecutorSummary
  plannedStartDate?: string
  plannedFinishDate?: string
  blockedReason?: string
  updatedAt: string
  version: number
}

export interface ProductionTasksPageResponse {
  items: ProductionTaskListRowResponse[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface ProductionTaskHistoryEventResponse {
  type: string
  actorDisplayName: string
  eventAt: string
  fromStatus?: ProductionTaskStatus | null
  toStatus?: ProductionTaskStatus | null
  note?: string | null
  reason?: string | null
}

export interface ProductionTaskDetailResponse extends ProductionTaskListRowResponse {
  allowedActions: ProductionTaskAction[]
  history: ProductionTaskHistoryEventResponse[]
  createdAt: string
}

export interface ProductionTaskListFilters {
  search?: string
  status?: ProductionTaskStatus
  orderId?: string
  orderItemId?: string
  executorUserId?: string
  assignedToMe?: boolean
  blockedOnly?: boolean
  activeOnly?: boolean
  dueDateFrom?: string
  dueDateTo?: string
  page?: number
  size?: number
  sort?: string
}

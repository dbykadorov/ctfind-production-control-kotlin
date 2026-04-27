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
  /** Set when status is BLOCKED — use for unblock target `toStatus`. */
  previousActiveStatus?: ProductionTaskStatus | null
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

export type ProductionTaskHistoryEventType =
  | 'CREATED'
  | 'ASSIGNED'
  | 'PLANNING_UPDATED'
  | 'STATUS_CHANGED'
  | 'BLOCKED'
  | 'UNBLOCKED'
  | 'COMPLETED'

export interface ProductionTaskHistoryEventResponse {
  type: ProductionTaskHistoryEventType | string
  actorDisplayName: string
  eventAt: string
  fromStatus?: ProductionTaskStatus | null
  toStatus?: ProductionTaskStatus | null
  previousExecutorDisplayName?: string | null
  newExecutorDisplayName?: string | null
  plannedStartDateBefore?: string | null
  plannedStartDateAfter?: string | null
  plannedFinishDateBefore?: string | null
  plannedFinishDateAfter?: string | null
  note?: string | null
  reason?: string | null
}

export interface ProductionTaskDetailResponse extends ProductionTaskListRowResponse {
  allowedActions: ProductionTaskAction[]
  history: ProductionTaskHistoryEventResponse[]
  createdAt: string
}

export interface CreateProductionTaskFromOrderLinePayload {
  orderItemId: string
  purpose: string
  quantity: number
  uom: string
  executorUserId?: string
  plannedStartDate?: string
  plannedFinishDate?: string
}

export interface CreatedProductionTaskItemResponse {
  id: string
  taskNumber: string
  status: ProductionTaskStatus
  version: number
}

export interface CreateProductionTasksFromOrderResponse {
  items: CreatedProductionTaskItemResponse[]
}

export interface PutProductionTaskAssignmentPayload {
  expectedVersion: number
  executorUserId: string
  plannedStartDate?: string
  plannedFinishDate?: string
  note?: string
}

export interface PostProductionTaskStatusPayload {
  expectedVersion: number
  toStatus: ProductionTaskStatus
  reason?: string
  note?: string
}

export interface ProductionTaskAssigneeRow {
  id: string
  displayName: string
  login: string
}

export interface ProductionTaskAssigneesResponse {
  items: ProductionTaskAssigneeRow[]
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

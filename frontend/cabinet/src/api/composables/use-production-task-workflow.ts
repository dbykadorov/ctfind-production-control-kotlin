import type { ProductionTaskAction, ProductionTaskStatus } from '@/api/types/production-tasks'

export interface ProductionTaskStatusPayload {
  expectedVersion: number
  toStatus: ProductionTaskStatus
  note?: string
  reason?: string
}

export function availableProductionTaskActions(actions: readonly ProductionTaskAction[]): ProductionTaskAction[] {
  return [...actions]
}

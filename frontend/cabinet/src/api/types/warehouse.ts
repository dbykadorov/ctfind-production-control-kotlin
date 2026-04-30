export type MeasurementUnit = 'PIECE' | 'KILOGRAM' | 'METER' | 'LITER' | 'SQUARE_METER' | 'CUBIC_METER'

export type MovementType = 'RECEIPT' | 'CONSUMPTION'

export interface MaterialResponse {
  id: string
  name: string
  unit: MeasurementUnit
  currentStock: number
  createdAt: string
  updatedAt: string
}

export interface MaterialsPageResponse {
  items: MaterialResponse[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface StockMovementResponse {
  id: string
  materialId: string
  materialName?: string | null
  materialUnit?: MeasurementUnit | null
  movementType: MovementType
  orderId?: string | null
  orderNumber?: string | null
  quantity: number
  comment: string | null
  actorDisplayName: string
  createdAt: string
}

export interface StockMovementsPageResponse {
  items: StockMovementResponse[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface CreateMaterialRequest {
  name: string
  unit: MeasurementUnit
}

export interface UpdateMaterialRequest {
  name: string
  unit: MeasurementUnit
}

export interface StockReceiptRequest {
  quantity: number
  comment?: string
}

export interface ConsumeRequest {
  orderId: string
  quantity: number
  comment?: string
}

export interface BomLine {
  id: string
  orderId: string
  materialId: string
  materialName: string
  materialUnit: MeasurementUnit
  quantity: number
  comment: string | null
  createdAt: string
  updatedAt: string
}

export interface BomLineListResponse {
  items: BomLine[]
}

export interface BomLineCreateRequest {
  materialId: string
  quantity: number
  comment?: string
}

export interface BomLineUpdateRequest {
  quantity: number
  comment?: string
}

export interface MaterialUsageRow {
  materialId: string
  materialName: string
  materialUnit: MeasurementUnit
  requiredQuantity: number
  consumedQuantity: number
  remainingToConsume: number
  overconsumption: number
}

export interface MaterialUsage {
  orderId: string
  rows: MaterialUsageRow[]
}

export type InventoryOrderStatus = 'NEW' | 'IN_WORK' | 'READY' | 'SHIPPED'

export interface InventoryOrderSummary {
  id: string
  orderNumber: string
  customerName: string
  status: InventoryOrderStatus
}

export interface InventoryOrderList {
  items: InventoryOrderSummary[]
}

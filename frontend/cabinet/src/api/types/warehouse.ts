export type MeasurementUnit = 'PIECE' | 'KILOGRAM' | 'METER' | 'LITER' | 'SQUARE_METER' | 'CUBIC_METER'

export type MovementType = 'RECEIPT'

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
  movementType: MovementType
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

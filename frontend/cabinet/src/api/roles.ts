export const BACKEND_ROLE_CODES = {
  admin: 'ADMIN',
  orderManager: 'ORDER_MANAGER',
  warehouse: 'WAREHOUSE',
  productionSupervisor: 'PRODUCTION_SUPERVISOR',
  productionExecutor: 'PRODUCTION_EXECUTOR',
} as const

export const LEGACY_ROLE_LABELS = {
  administrator: 'Administrator',
  systemManager: 'System Manager',
  orderManager: 'Order Manager',
  shopSupervisor: 'Shop Supervisor',
  executor: 'Executor',
  warehouse: 'Warehouse',
  orderCorrector: 'Order Corrector',
} as const

export const ADMIN_USER_LOGIN = 'Administrator'

export const ROUTE_ROLE_GROUPS = {
  adminOnly: [
    BACKEND_ROLE_CODES.admin,
  ],
  orderManagerOnly: [
    LEGACY_ROLE_LABELS.orderManager,
    BACKEND_ROLE_CODES.orderManager,
  ],
  orderRead: [
    LEGACY_ROLE_LABELS.orderManager,
    LEGACY_ROLE_LABELS.shopSupervisor,
    LEGACY_ROLE_LABELS.orderCorrector,
    LEGACY_ROLE_LABELS.warehouse,
    BACKEND_ROLE_CODES.orderManager,
    BACKEND_ROLE_CODES.productionSupervisor,
    BACKEND_ROLE_CODES.warehouse,
  ],
  productionWork: [
    LEGACY_ROLE_LABELS.orderManager,
    LEGACY_ROLE_LABELS.shopSupervisor,
    LEGACY_ROLE_LABELS.executor,
    BACKEND_ROLE_CODES.orderManager,
    BACKEND_ROLE_CODES.productionSupervisor,
    BACKEND_ROLE_CODES.productionExecutor,
  ],
  warehouseOnly: [
    BACKEND_ROLE_CODES.admin,
    BACKEND_ROLE_CODES.warehouse,
  ],
} satisfies Record<string, string[]>

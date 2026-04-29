# Tasks: Склад — материалы и остатки

**Input**: Design documents from `specs/011-warehouse-materials/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/materials-api.md, quickstart.md

**Tests**: Included — source-text TDD tests following project conventions (readFileSync + string assertions) for frontend; JUnit 5 + MockK for backend application layer.

**Organization**: US1 (Material CRUD) and US3 (Sidebar nav) are merged into Phase 3 — the sidebar + route are needed to test US1 independently. US2 (Receipt + Journal) builds on US1. US4 (Audit) is integrated into the use cases of US1/US2 and the audit adapter is a separate Phase 5 concern.

**Constitution**: Domain entities have no Spring/JPA dependencies. Business rules (name uniqueness, deletion guard, quantity > 0) live in domain/application code. AuditCategory.INVENTORY added to shared enum. Frontend components handle display only.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Paths: `frontend/cabinet/src/` for frontend, `src/main/kotlin/com/ctfind/productioncontrol/` for backend

---

## Phase 1: Setup

**Purpose**: Frontend types, i18n keys — shared foundation for all frontend phases

- [X] T001 [P] Create warehouse TypeScript types in `frontend/cabinet/src/api/types/warehouse.ts`: MaterialResponse (id, name, unit, currentStock, createdAt, updatedAt), MeasurementUnit enum (PIECE/KILOGRAM/METER/LITER/SQUARE_METER/CUBIC_METER), StockMovementResponse, MaterialsPageResponse, StockMovementsPageResponse, CreateMaterialRequest, UpdateMaterialRequest, StockReceiptRequest
- [X] T002 [P] Add warehouse i18n namespace to `frontend/cabinet/src/i18n/keys.ts`: nav.warehouse, meta.title.warehouse, meta.title.warehouseMaterial, warehouse.{title, addMaterial, editMaterial, receipt, movements, emptyMaterials, search, deleteMaterial, confirmDelete, hasMovementsError, duplicateNameError}, warehouse.units.{PIECE, KILOGRAM, METER, LITER, SQUARE_METER, CUBIC_METER}, warehouse.fields.{name, unit, currentStock, quantity, comment, actorDisplayName}, warehouse.movement.{RECEIPT}
- [X] T003 [P] Add Russian translations to `frontend/cabinet/src/i18n/ru.ts` for all keys added in T002: nav.warehouse='Склад', units: шт/кг/м/л/м²/м³, movement.RECEIPT='Приход', all UI labels

---

## Phase 2: Foundational (Backend — блокирует US1-US4)

**Purpose**: DB migration, AuditCategory enum extension, domain entities — MUST be complete before user story implementation

**⚠️ CRITICAL**: Without migration V8 the application won't boot; without AuditCategory.INVENTORY audit events throw IllegalArgumentException at runtime.

- [X] T004 Create Flyway migration `src/main/resources/db/migration/V8__create_inventory_tables.sql`: CREATE TABLE material (id UUID PK, name VARCHAR(255) UNIQUE NOT NULL, unit VARCHAR(20) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL), CREATE TABLE stock_movement (id UUID PK, material_id UUID FK → material, movement_type VARCHAR(20) NOT NULL, quantity NUMERIC(15,4) NOT NULL CHECK(quantity>0), comment VARCHAR(500), actor_user_id UUID NOT NULL, actor_display_name VARCHAR(255) NOT NULL, created_at TIMESTAMPTZ NOT NULL), CREATE TABLE inventory_audit_event (id UUID PK, event_type VARCHAR(50) NOT NULL, actor_user_id UUID NOT NULL, target_id UUID NOT NULL, event_at TIMESTAMPTZ NOT NULL, summary VARCHAR(500) NOT NULL, metadata TEXT), indexes on stock_movement(material_id) and (created_at DESC), INSERT INTO role (WAREHOUSE role seed with ON CONFLICT DO NOTHING)
- [X] T005 Add `INVENTORY` to `AuditCategory` enum in `src/main/kotlin/com/ctfind/productioncontrol/audit/domain/AuditLogModels.kt`: change `enum class AuditCategory { AUTH, ORDER, PRODUCTION_TASK, INVENTORY }`
- [X] T006 Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/domain/Material.kt`: data class Material(val id: UUID = UUID.randomUUID(), val name: String, val unit: MeasurementUnit, val createdAt: Instant, val updatedAt: Instant) with init block validating name.isNotBlank(). Also define enum class MeasurementUnit { PIECE, KILOGRAM, METER, LITER, SQUARE_METER, CUBIC_METER }
- [X] T007 Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/domain/StockMovement.kt`: data class StockMovement(val id: UUID, val materialId: UUID, val movementType: MovementType, val quantity: BigDecimal, val comment: String?, val actorUserId: UUID, val actorDisplayName: String, val createdAt: Instant) with init block validating quantity > 0. Also define enum class MovementType { RECEIPT }
- [X] T008 Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/domain/InventoryAuditEvent.kt`: data class InventoryAuditEvent(val id: UUID = UUID.randomUUID(), val eventType: String, val actorUserId: UUID, val targetId: UUID, val eventAt: Instant = Instant.now(), val summary: String, val metadata: String? = null)
- [X] T009 Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/InventoryPorts.kt`: interfaces MaterialPort (findById, findAll with pageable+search, save, deleteById, existsByNameIgnoreCase, hasMovements), StockMovementPort (save, findByMaterialId with pageable, sumQuantityByMaterialId), InventoryAuditPort (record(event: InventoryAuditEvent))
- [X] T010 Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/InventoryModels.kt`: sealed interface InventoryMutationResult<out T> (Success, Forbidden, NotFound, ValidationFailed, Conflict), data classes CreateMaterialCommand (name, unit, actor), UpdateMaterialCommand (id, name, unit, actor), DeleteMaterialCommand (id, actor), ReceiveStockCommand (materialId, quantity, comment, actor), MaterialListQuery (search, page, size), data class AuthenticatedInventoryActor (userId, roleCodes), data class MaterialView (id, name, unit, currentStock, createdAt, updatedAt), data class MaterialsPageResult (items, page, size, totalItems, totalPages), data class StockMovementView and StockMovementsPageResult
- [X] T011 Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/InventoryPermissions.kt`: const val WAREHOUSE_ROLE_CODE = "WAREHOUSE", fun canManageInventory(roleCodes: Set<String>): Boolean = roleCodes.any { it == ADMIN_ROLE_CODE || it == WAREHOUSE_ROLE_CODE }

**Checkpoint**: Domain entities + ports defined. Backend application layer can now be built.

---

## Phase 3: User Story 1+3 — Справочник материалов + навигация (Priority: P1) 🎯 MVP

**Goal**: Full CRUD for materials (backend + frontend), list page with search/pagination, create/edit dialog, sidebar nav item, route.

**Independent Test**: Login as WAREHOUSE user → пункт «Склад» в Sidebar → открыть список → создать «Фанера» (м²) → остаток 0 → редактировать → удалить (без движений) → 403 при попытке без роли.

### Tests for US1+US3

- [X] T012 [P] [US1] Create source-text test for CreateMaterialUseCase in `src/test/kotlin/com/ctfind/productioncontrol/inventory/application/CreateMaterialUseCaseTests.kt`: verify Forbidden when actor lacks WAREHOUSE role, Success when valid input, ValidationFailed when name blank, Conflict when name duplicate
- [X] T013 [P] [US1] Create source-text test for DeleteMaterialUseCase in `src/test/kotlin/com/ctfind/productioncontrol/inventory/application/DeleteMaterialUseCaseTests.kt`: verify Success when no movements, Conflict when movements exist, NotFound, Forbidden
- [X] T014 [P] [US1] Create source-text test for WarehouseListPage in `frontend/cabinet/tests/unit/pages/WarehouseListPage.test.ts`: verify file contains useMaterials composable import, renders table with material columns, has search input, has Add button, uses usePermissions, renders with pagination controls, has receipt button per row
- [X] T015 [P] [US1] Create source-text test for MaterialCreateDialog in `frontend/cabinet/tests/unit/components/MaterialCreateDialog.test.ts`: verify file contains name input, unit select, submit button, emits created event, imports httpClient

### Implementation for US1+US3

- [X] T016 [US1] Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/CreateMaterialUseCase.kt`: @Service, inject MaterialPort + InventoryAuditPort, check canManageInventory, validate name not blank, check existsByNameIgnoreCase (Conflict if true), save Material, record InventoryAuditEvent(MATERIAL_CREATED), return Success(MaterialView)
- [X] T017 [US1] Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/UpdateMaterialUseCase.kt`: @Service, inject MaterialPort + InventoryAuditPort, check canManageInventory, findById (NotFound), validate name, check uniqueness excluding self, save, record MATERIAL_UPDATED, return Success(MaterialView)
- [X] T018 [US1] Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/DeleteMaterialUseCase.kt`: @Service, inject MaterialPort + StockMovementPort + InventoryAuditPort, check canManageInventory, findById (NotFound), check hasMovements (Conflict), deleteById, record MATERIAL_DELETED, return Success(Unit)
- [X] T019 [US1] Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/InventoryQueryUseCase.kt`: @Service, inject MaterialPort + StockMovementPort, fun listMaterials(query): MaterialsPageResult with currentStock computed via sumQuantityByMaterialId per item, fun getMaterial(id): MaterialView?, fun listMovements(materialId, page, size): StockMovementsPageResult
- [X] T020 [US1] Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/persistence/InventoryJpaEntities.kt`: @Entity MaterialJpaEntity (id, name, unit VARCHAR, createdAt, updatedAt) with @Table(name="material"), @Entity StockMovementJpaEntity (id, materialId, movementType, quantity, comment, actorUserId, actorDisplayName, createdAt) with @Table(name="stock_movement"), @Entity InventoryAuditEventJpaEntity (id, eventType, actorUserId, targetId, eventAt, summary, metadata) with @Table(name="inventory_audit_event")
- [X] T021 [US1] Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/persistence/InventoryJpaRepositories.kt`: interface MaterialJpaRepository : JpaRepository (findByNameIgnoreCaseAndIdNot, existsByNameIgnoreCase, countByIdIsNotNull), interface StockMovementJpaRepository (findByMaterialIdOrderByCreatedAtDesc with Pageable, sumQuantityByMaterialId @Query), interface InventoryAuditEventJpaRepository
- [X] T022 [US1] Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/persistence/InventoryPersistenceAdapter.kt`: @Component implementing MaterialPort, StockMovementPort, InventoryAuditPort — toEntity/toDomain mappers for Material ↔ JPA, MeasurementUnit mapping from/to String, MovementType mapping, hasMovements implementation via count query
- [X] T023 [US1] Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/web/InventoryDtos.kt`: data class CreateMaterialRequest (@field:NotBlank name, @field:NotNull unit), UpdateMaterialRequest, StockReceiptRequest (@field:Positive quantity, comment), MaterialResponse (id, name, unit, currentStock, createdAt, updatedAt), StockMovementResponse (id, materialId, movementType, quantity, comment, actorDisplayName, createdAt), MaterialsPageResponse, StockMovementsPageResponse, InventoryApiErrorResponse. Extension funs: MaterialView.toResponse(), StockMovementView.toResponse()
- [X] T024 [US1] Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/web/InventoryController.kt`: @RestController @RequestMapping("/api/materials"), inject query/create/update/delete/receive use cases, GET / (list with ?page&size&search), POST / (create), GET /{id} (single), PUT /{id} (update), DELETE /{id} (delete), POST /{id}/receipt, GET /{id}/movements. Extract Jwt.toInventoryActor(). Map InventoryMutationResult sealed cases to HTTP responses (201/200/204/400/403/404/409).
- [X] T025 [US1] Create `frontend/cabinet/src/api/composables/use-materials.ts`: useMaterials(params?: {page?, size?, search?}) → {data: Ref<MaterialsPageResponse|null>, loading, error, refetch} following useAuditLog pattern with AbortController. Separate function useMaterialDetail(id) → {data, loading, error, refetch}. API client: GET /api/materials, GET /api/materials/:id
- [X] T026 [P] [US1] Create `frontend/cabinet/src/components/domain/warehouse/MaterialCreateDialog.vue`: Dialog wrapping form with name input (required) + unit select (options from MeasurementUnit enum with RU labels), submit POST /api/materials, emits 'created' on success, shows server validation errors, loading state on button
- [X] T027 [US1] Create `frontend/cabinet/src/pages/warehouse/WarehouseListPage.vue`: following AuditLogPage pattern — header with title + "Добавить материал" Button, search input (debounced), loading Skeleton (5 rows), error banner, empty state, table with columns (название, ед.изм., остаток, действия), action buttons per row (Редактировать / Приход / Удалить with confirmDelete guard), pagination (prev/next, "стр X из Y"), MaterialCreateDialog integration, route DELETE to use httpClient.delete
- [X] T028 [P] [US3] Add warehouse route to `frontend/cabinet/src/router/index.ts`: { path: 'warehouse', name: 'warehouse.list', component: () => import('@/pages/warehouse/WarehouseListPage.vue'), meta: { title: 'meta.title.warehouse' } }, { path: 'warehouse/:id', name: 'warehouse.material', component: () => import('@/pages/warehouse/MaterialDetailPage.vue'), props: true, meta: { title: 'meta.title.warehouseMaterial', showBackButton: true, backPath: '/cabinet/warehouse' } }
- [X] T029 [P] [US3] Add "Склад" NavItem to Sidebar in `frontend/cabinet/src/components/layout/Sidebar.vue`: import Package from lucide-vue-next, add item { to: '/cabinet/warehouse', icon: Package, key: 'nav.warehouse', visible: permissions.value.isWarehouse || permissions.value.isAdmin } before or after audit item

**Checkpoint**: Materials CRUD functional. Login as WAREHOUSE user → Склад in Sidebar → create/edit/delete materials.

---

## Phase 4: User Story 2 — Приход материала + журнал движений (Priority: P1)

**Goal**: Receipt operation + stock level display + movement journal per material (detail page).

**Independent Test**: From materials list → click Приход on «Фанера» → input 100 → остаток = 100 → input 50.5 → остаток = 150.5 → open material detail → see 2 movement entries in reverse chronological order.

### Tests for US2

- [X] T030 [P] [US2] Create source-text test for ReceiveStockUseCase in `src/test/kotlin/com/ctfind/productioncontrol/inventory/application/ReceiveStockUseCaseTests.kt`: verify Forbidden when no WAREHOUSE role, NotFound when material missing, Success creates StockMovement with correct quantity, Success records STOCK_RECEIPT audit event, ValidationFailed when quantity <= 0
- [X] T031 [P] [US2] Create source-text test for StockReceiptDialog in `frontend/cabinet/tests/unit/components/StockReceiptDialog.test.ts`: verify file contains quantity input, optional comment textarea, submit button, emits received event, POST to /api/materials/:id/receipt, quantity validation (> 0)

### Implementation for US2

- [X] T032 [US2] Create `src/main/kotlin/com/ctfind/productioncontrol/inventory/application/ReceiveStockUseCase.kt`: @Service, inject MaterialPort + StockMovementPort + InventoryAuditPort, check canManageInventory, findById (NotFound), validate quantity > 0 (ValidationFailed), save StockMovement(RECEIPT), record InventoryAuditEvent(STOCK_RECEIPT, summary includes material name + quantity + comment if present), return Success(StockMovementView)
- [X] T033 [P] [US2] Create `frontend/cabinet/src/components/domain/warehouse/StockReceiptDialog.vue`: Dialog with quantity input (type=number, min=0.0001, required), comment textarea (optional), POST /api/materials/:id/receipt, emits 'received' on success, shows validation error for quantity <= 0, loading state
- [X] T034 [US2] Create `frontend/cabinet/src/pages/warehouse/MaterialDetailPage.vue`: props: { id: string }, load material via useMaterialDetail(id), load movements via useMaterialMovements(id), show material card (name, unit, currentStock), StockReceiptDialog trigger button, table of movements (дата, тип, количество, ед.изм., комментарий, кто оформил), pagination, back navigation to /cabinet/warehouse
- [X] T035 [US2] Integrate StockReceiptDialog into WarehouseListPage in `frontend/cabinet/src/pages/warehouse/WarehouseListPage.vue`: bind Приход button in action column to open StockReceiptDialog with materialId, on 'received' event call refetch() to update currentStock in table

**Checkpoint**: Receipt functional. currentStock updates after receipt. Movement journal visible in material detail.

---

## Phase 5: User Story 4 — Аудит складских операций (Priority: P2)

**Goal**: All inventory operations appear in the unified audit log under AuditCategory.INVENTORY.

**Independent Test**: Admin → Журнал действий → filter by category Склад → see MATERIAL_CREATED, MATERIAL_UPDATED, MATERIAL_DELETED, STOCK_RECEIPT events with actor names and summaries.

- [X] T036 [US4] Add `fetchInventoryEvents()` to `src/main/kotlin/com/ctfind/productioncontrol/audit/adapter/persistence/AuditPersistenceAdapter.kt`: add `if (AuditCategory.INVENTORY in categories) rows += fetchInventoryEvents(query)` alongside existing ORDER/PRODUCTION_TASK fetches. Implement fetchInventoryEvents: SELECT from inventory_audit_event join users for displayName, map to AuditLogRow with category=INVENTORY, targetType="MATERIAL", targetId from event
- [X] T037 [US4] Verify audit category filter works in frontend by checking `frontend/cabinet/src/pages/audit/AuditLogPage.vue` and related composables — confirm INVENTORY is handled in category display (may need to add label if hardcoded). Add 'INVENTORY' → 'Склад' mapping in audit i18n if needed in `frontend/cabinet/src/i18n/ru.ts`

**Checkpoint**: Audit log shows INVENTORY category events. Admin can filter and see all inventory operations.

---

## Phase 6: Polish & Verification

**Purpose**: Cross-cutting verification

- [X] T038 Run `make backend-test-docker` — verify all backend tests pass including new inventory tests (T012, T013, T030) and existing tests unaffected by AuditCategory change
- [X] T039 Run `pnpm typecheck` in `frontend/cabinet/` — no TypeScript errors
- [X] T040 Run `pnpm test` in `frontend/cabinet/` — all tests pass (existing + new T014, T015, T031)
- [X] T041 Run `pnpm build` in `frontend/cabinet/` — production build succeeds
- [ ] T042 Run `make docker-up-detached && make health` — stack healthy, V8 migration applied
- [ ] T043 Run quickstart.md manual verification: create materials → receipt → journal → audit log → WAREHOUSE role visibility

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — T001-T003 all parallel
- **Foundational (Phase 2)**: No dependency on Phase 1 (backend-only), BLOCKS all US phases
- **US1+US3 (Phase 3)**: Depends on Phase 1 (types, i18n) + Phase 2 (domain, ports)
- **US2 (Phase 4)**: Depends on Phase 2 (ReceiveStockUseCase needs MaterialPort) + T027 (WarehouseListPage for integration T035)
- **US4 (Phase 5)**: Depends on Phase 2 (AuditCategory.INVENTORY) + T016-T018, T032 (audit calls in use cases)
- **Polish (Phase 6)**: Depends on all phases complete

### Within Each Phase

- Domain entities (T006-T008) before ports (T009-T011)
- Ports before use cases (T016-T019, T032)
- Use cases before persistence adapter (T022) before controller (T024)
- Controller before frontend composable (T025)
- Composable before page components (T027, T034)

### Parallel Opportunities

- T001 + T002 + T003 — Phase 1 all parallel
- T005 + T006 + T007 + T008 — foundational domain files parallel (different files)
- T009 + T010 + T011 — parallel (different files)
- T012 + T013 + T014 + T015 — test stubs parallel (different files)
- T020 + T021 — parallel (different files)
- T026 + T028 + T029 — parallel (different files, no inter-dependency)
- T030 + T031 — parallel (different test files)
- T033 + T034 can start in parallel (different components, T035 connects them after)

---

## Implementation Strategy

### MVP First (US1+US3 Only)

1. Phase 1: Setup (types, i18n) — all parallel
2. Phase 2: Foundational (migration, domain entities, ports)
3. Phase 3: Material CRUD + Sidebar + Route
4. **STOP and VALIDATE**: WAREHOUSE user → list → create → edit → delete → 403 without role

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1+US3 (Material CRUD + Nav) → test → verify ✅ (MVP!)
3. US2 (Receipt + Journal) → test → verify ✅
4. US4 (Audit) → test → verify ✅
5. Polish → full verification

---

## Notes

- US1 and US3 merged into one phase because sidebar + route are required for independent testing of US1
- No new npm dependencies — reusing existing Dialog, lucide-vue-next icons, axios, Vue-i18n
- Migration V8 — V7 is reserved for notification targetEntityId (spec 010, not yet merged to main)
- Frontend tests use source-text TDD pattern (readFileSync + string assertions), NOT component mounting
- currentStock is computed on the backend (SUM of movements), never stored denormalized
- AuditCategory.INVENTORY is a non-breaking enum extension (existing enum values unchanged)
- WAREHOUSE role seeded in migration — no manual setup needed in new environments

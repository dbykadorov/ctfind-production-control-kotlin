# Implementation Plan: Списание материалов под заказ + BOM

**Branch**: `012-order-bom-consumption` | **Date**: 2026-04-29 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/012-order-bom-consumption/spec.md`

## Summary

Расширение модуля `inventory` (Phase 1, спека 011) до полного цикла «заказ → план → списание → расход». Добавляется новая сущность **OrderMaterialRequirement** (BOM-строка), новый тип движения `CONSUMPTION` с обязательной привязкой к заказу, и агрегированное представление расхода по заказу. Cross-module-доступ inventory → orders реализуется через новый порт `OrderLookupPort` (по образцу `ProductionOrderSourcePort`). Frontend — две новые секции в `OrderDetailPage` плюс альтернативный вход списания из `WarehouseListPage`.

## Technical Context

**Language/Version**: Backend — Kotlin 2.x, Java 21, Spring Boot. Frontend — TypeScript (strict), Vue 3.4+, Vite, Pinia.
**Primary Dependencies**: Spring MVC, Spring Data JPA, Flyway 11, JUnit 5 + MockK 1.13. Frontend — vue-router 4, vue-i18n 9, lucide-vue-next, Tailwind, vitest 2, @vue/test-utils.
**Storage**: PostgreSQL 16 через Flyway миграции. JPA `ddl-auto=validate`.
**Testing**: Backend — JUnit 5, MockK для use case-тестов; интеграционные тесты через Spring + PostgreSQL контейнер. Frontend — Vitest + source-text TDD (readFileSync + string assertions) по сложившемуся паттерну в `frontend/cabinet/tests/unit/`.
**Target Platform**: Linux server (Spring Boot fat JAR в Docker), браузеры Chromium/Firefox/Safari актуальных версий для Cabinet SPA.
**Project Type**: Modular monolith (single Gradle module) + отдельный Vue SPA в `frontend/cabinet/`.
**Performance Goals**: Соответствие SLO кабинета (наследуется): API-ответы p95 ≤ 500ms на типичной нагрузке; UI-секция расхода — без видимой задержки при ≤ 100 строк BOM на заказ.
**Constraints**: BOM-строк в одном заказе ожидается ≤ 50 на практике; списаний по заказу за всё его время — ≤ 200; всё помещается в один-два паджинированных запроса. Списание выполняется в одной транзакции с проверкой остатка; конкурентные списания одного материала сериализуются на уровне строки `material`.
**Scale/Scope**: Spec 012 затрагивает 1 backend-модуль (`inventory`) + 1 cross-module порт + 1 frontend-секцию (страница `OrderDetailPage`) + 1 переиспользуемая модалка. Без Bedrock/Vertex/MCP — это ERP-сервис.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **ERP domain fit** ✅ — Спека напрямую укрепляет связку «склад ↔ заказы». Закрывает критичный для ERP сценарий «учёт расхода материалов на заказ» и встраивает его в существующую доменную модель `customer_order` без дублирования сущностей.
- **Constraint-aware operations** ✅ — Все факты для будущего TOC сохраняются: каждое списание — отдельная запись `stock_movement` с timestamp, актором, заказом, материалом и количеством. Не вводится FIFO-сортировка или жёсткий приоритет; агрегат расхода вычисляется честно из движений. История перерасхода доступна для будущего bottleneck-анализа.
- **Architecture boundaries** ✅ — Бизнес-правила (валидация остатка, блокировка по статусу заказа, уникальность BOM, правило удаления BOM-строки при наличии списаний) живут в `inventory/application/*UseCase` и `inventory/domain` policies. Контроллеры — только маршрутизация и DTO. Cross-module — через порт `OrderLookupPort`, без прямого импорта `orders.adapter.persistence.*`.
- **Traceability/audit** ✅ — Все мутации (BOM_LINE_ADDED / BOM_LINE_UPDATED / BOM_LINE_REMOVED / STOCK_CONSUMPTION) пишутся в существующую таблицу `inventory_audit_event` через существующий `InventoryAuditPort`. Поле `metadata` (TEXT) используется для diff old→new при `BOM_LINE_UPDATED`.
- **API-only/security** ✅ — Бэкенд остаётся API-only. Все новые endpoints под JWT Bearer. RBAC явно прописан: BOM-edit — `ADMIN ∪ ORDER_MANAGER`; consume — `ADMIN ∪ WAREHOUSE`; view BOM/usage — `ADMIN ∪ ORDER_MANAGER ∪ WAREHOUSE ∪ PRODUCTION_SUPERVISOR`. Никаких неявных усилений или ослаблений security.
- **Docker/verifiability** ✅ — Изменения только в backend-модуле и frontend-чанке; `docker-compose.yml`, ports, healthcheck не меняются. Verification = `make backend-test-docker`, `pnpm test` + `pnpm typecheck`, `make docker-up-detached && make health`, ручной quickstart по [quickstart.md](./quickstart.md).
- **Exception handling** — нет нарушений. Complexity Tracking пуст.

## Project Structure

### Documentation (this feature)

```text
specs/012-order-bom-consumption/
├── plan.md              # this file
├── research.md          # Phase 0 — ключевые архитектурные решения и почему
├── data-model.md        # Phase 1 — сущности, ER-диаграмма, инварианты
├── quickstart.md        # Phase 1 — runbook ручной проверки
├── contracts/
│   ├── rest-bom.md           # REST contract: /api/orders/{id}/bom
│   ├── rest-consumption.md   # REST contract: /api/materials/{id}/consume + /api/orders/{id}/material-usage
│   ├── order-lookup-port.md  # cross-module Kotlin port contract
│   └── openapi.yaml          # машиночитаемая спека всех endpoints
├── checklists/
│   └── requirements.md  # из /speckit-specify
└── tasks.md             # сгенерируется /speckit-tasks
```

### Source Code (repository root)

```text
src/main/kotlin/com/ctfind/productioncontrol/
├── inventory/                                    # ← ВСЕ изменения backend
│   ├── domain/
│   │   ├── Material.kt                           # без изменений
│   │   ├── StockMovement.kt                      # + MovementType.CONSUMPTION, + orderId: UUID? 
│   │   ├── OrderMaterialRequirement.kt           # NEW — BOM-строка
│   │   └── InventoryAuditEvent.kt                # без изменений (используется как есть)
│   ├── application/
│   │   ├── InventoryPorts.kt                     # + OrderLookupPort, + OrderMaterialRequirementPort, + StockMovementPort расширен
│   │   ├── InventoryModels.kt                    # + BomLineView, + MaterialUsageView, + commands
│   │   ├── InventoryQueryUseCase.kt              # + listBom(orderId), + getMaterialUsage(orderId)
│   │   ├── InventoryPermissions.kt               # + canEditBom, + canViewBomAndUsage
│   │   ├── ReceiveStockUseCase.kt                # без изменений (orderId всегда null)
│   │   ├── ConsumeStockUseCase.kt                # NEW — списание с привязкой к заказу
│   │   ├── AddBomLineUseCase.kt                  # NEW
│   │   ├── UpdateBomLineUseCase.kt               # NEW
│   │   └── RemoveBomLineUseCase.kt               # NEW
│   └── adapter/
│       ├── web/
│       │   ├── InventoryController.kt            # + POST /materials/{id}/consume
│       │   ├── BomController.kt                  # NEW — /api/orders/{orderId}/bom*
│       │   ├── MaterialUsageController.kt        # NEW — /api/orders/{orderId}/material-usage
│       │   └── InventoryDtos.kt                  # + DTO для всех новых эндпоинтов
│       └── persistence/
│           ├── InventoryJpaEntities.kt           # + OrderMaterialRequirementEntity, + StockMovementEntity.orderId
│           ├── InventoryJpaRepositories.kt       # + OrderMaterialRequirementJpaRepository
│           ├── InventoryPersistenceAdapter.kt    # реализует все новые порты, оставаясь единым @Component
│           └── OrderLookupAdapter.kt             # NEW — реализация OrderLookupPort через CustomerOrderJpaRepository
│
src/main/resources/db/migration/
└── V9__bom_and_consumption.sql                   # NEW

src/test/kotlin/com/ctfind/productioncontrol/inventory/
├── domain/                                       # инварианты OrderMaterialRequirement
├── application/                                  # use case-тесты с MockK
└── adapter/web/                                  # controller-тесты (MockMvc)

frontend/cabinet/src/
├── api/types/inventory.ts                        # + BomLine, MaterialUsage types
├── api/composables/
│   ├── use-order-bom.ts                          # NEW — CRUD BOM
│   ├── use-order-material-usage.ts               # NEW — расход по заказу
│   └── use-stock-consumption.ts                  # NEW — списание (общая для двух точек входа)
├── components/domain/orders/
│   ├── BomSection.vue                            # NEW — таблица + кнопка добавления
│   ├── BomLineDialog.vue                         # NEW — модалка add/edit
│   └── MaterialUsageSection.vue                  # NEW — агрегат расхода
├── components/domain/warehouse/
│   └── StockConsumeDialog.vue                    # NEW — модалка списания (используется из обеих точек)
├── pages/office/OrderDetailPage.vue              # + интеграция BomSection + MaterialUsageSection
└── pages/warehouse/WarehouseListPage.vue         # + кнопка «Списать» (открывает StockConsumeDialog с пикером заказа)

frontend/cabinet/tests/unit/
├── components/BomSection.test.ts                 # NEW
├── components/StockConsumeDialog.test.ts         # NEW
├── composables/use-order-bom.test.ts             # NEW
└── pages/OrderDetailPage.test.ts                 # обновляется (добавляются новые секции)
```

**Structure Decision**: Web application (Option 2). Backend hexagonal-monolith — все изменения вносятся в существующий модуль `inventory/` без создания новых модулей. Cross-module доступ к `orders` — строго через порт `OrderLookupPort`, имплементация которого живёт **в `inventory/adapter/persistence/`** и читает `CustomerOrderJpaRepository`. Frontend — `frontend/cabinet/`, разделение по доменным папкам `components/domain/orders/` (BOM-UI принадлежит контексту заказа) и `components/domain/warehouse/` (модалка списания — общий компонент склада, переиспользуется из обеих точек входа).

## Complexity Tracking

> Нет нарушений Constitution Check. Таблица не заполняется.

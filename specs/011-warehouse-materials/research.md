# Research: Склад — материалы и остатки

**Date**: 2026-04-29 | **Branch**: `011-warehouse-materials`

## R-001: Роль WAREHOUSE в бэкенде

**Decision**: Создать константу `WAREHOUSE_ROLE_CODE = "WAREHOUSE"` в модуле inventory. Роль с code="WAREHOUSE", name="Warehouse" уже может существовать в БД (через seeder или ручное создание). Frontend уже маппит `'Warehouse'` → `isWarehouse`.

**Rationale**: Паттерн аналогичен `ORDER_MANAGER_ROLE_CODE`, `PRODUCTION_SUPERVISOR_ROLE_CODE` — каждый модуль определяет свои константы. Frontend уже готов — `use-permissions.ts` содержит `ROLE_WAREHOUSE = 'Warehouse'` и флаг `isWarehouse`.

**Alternatives considered**: Использовать существующую роль (нет подходящей), добавить в auth/domain (нарушает модульность).

## R-002: Хранение остатков — вычисляемое vs денормализованное

**Decision**: Текущий остаток вычисляется как `SUM(quantity)` из таблицы `stock_movement` по `material_id`. Денормализованный столбец `current_stock` в `material` НЕ создаётся.

**Rationale**: В Phase 1 объём данных мал (до 50 пользователей, сотни материалов, тысячи движений). SUM по индексированному FK — миллисекунды. Денормализация добавляет сложность синхронизации и риск рассогласования. Если в спеке 012 (списание) производительность станет проблемой — можно добавить materialized view или кэш.

**Alternatives considered**: Денормализованный столбец `current_stock` с обновлением в триггере или use case — отвергнут из-за сложности и риска рассогласования при конкурентных записях.

## R-003: Единицы измерения — enum vs строка

**Decision**: Kotlin enum `MeasurementUnit` с значениями: PIECE, KILOGRAM, METER, LITER, SQUARE_METER, CUBIC_METER. В БД хранится как VARCHAR (enum name). На фронте — select с локализованными названиями.

**Rationale**: Фиксированный набор из спеки (шт, кг, м, л, м², м³). Enum гарантирует валидность на уровне типов. Расширение — добавление значения в enum + миграция (но пока не нужно).

**Alternatives considered**: Свободная строка — отвергнута, т.к. приведёт к дубликатам ("шт", "штука", "штуки"). Отдельная таблица-справочник — overengineering для 6 значений.

## R-004: Структура таблиц БД

**Decision**: Две таблицы — `material` и `stock_movement`. Миграция V8 (V7 занята notification targetEntityId из спеки 010).

**Rationale**: Минимальная схема, соответствующая domain model. Движение хранит `movement_type` (VARCHAR) для расширяемости в спеке 012 (RECEIPT → RECEIPT + WRITE_OFF).

**Alternatives considered**: Одна таблица с колонкой остатка — отвергнута (см. R-002).

## R-005: Audit интеграция

**Decision**: Создать `InventoryAuditPort` интерфейс + `InventoryAuditEvent` domain event. В `AuditPersistenceAdapter` добавить fetchInventoryEvents() и новую категорию `INVENTORY` в `AuditCategory` enum. Паттерн аналогичен `OrderAuditPort`/`ProductionTaskAuditPort`.

**Rationale**: Все существующие модули (orders, production) используют этот паттерн. Единый аудит-лог с фильтрацией по категориям.

**Alternatives considered**: Отдельная таблица аудита для inventory — отвергнута (нарушает единый журнал).

## R-006: Удаление материала — стратегия

**Decision**: Hard delete с проверкой `COUNT(stock_movement WHERE material_id = ?) = 0`. Если есть движения — ошибка 409 Conflict.

**Rationale**: Spec явно требует "удалять материал только если у него нет записей движений". Soft delete добавляет сложность (фильтрация deleted записей везде), а при отсутствии движений hard delete безопасен — данные не теряются.

**Alternatives considered**: Soft delete (is_deleted flag) — overengineering для текущего scope.

## R-007: Seeder для роли WAREHOUSE

**Decision**: Добавить роль WAREHOUSE в Flyway миграцию V8 вместе с таблицами. INSERT INTO role (id, code, name, created_at) VALUES (uuid, 'WAREHOUSE', 'Warehouse', NOW()).

**Rationale**: Роль должна существовать для назначения пользователям. Seeder в миграции гарантирует наличие роли в любом окружении.

**Alternatives considered**: Ручное создание через UI — ненадёжно, забудется в новых окружениях.

## R-008: Frontend — модальные окна vs отдельные страницы

**Decision**: Создание/редактирование материала — Dialog (модальное окно). Приход — Dialog. Журнал движений — отдельная карточка материала (страница /cabinet/warehouse/:id).

**Rationale**: Материал имеет всего 2 поля (название + ед.изм.) — полная страница избыточна. Приход — 2 поля (количество + комментарий). Журнал движений содержит пагинированный список — лучше на отдельной странице с контекстом материала.

**Alternatives considered**: Всё на одной странице с inline-формами — перегружено. Всё в модалках — журнал движений плохо помещается в модалку.

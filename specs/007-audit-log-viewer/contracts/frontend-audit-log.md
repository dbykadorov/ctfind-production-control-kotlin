# Contract: Frontend Audit Log

**Feature**: 007-audit-log-viewer
**Date**: 2026-04-28

## TypeScript Types

```typescript
// frontend/cabinet/src/api/types/audit-log.ts

export type AuditCategory = 'AUTH' | 'ORDER' | 'PRODUCTION_TASK'

export interface AuditLogRowResponse {
  id: string
  occurredAt: string              // ISO-8601
  category: AuditCategory
  eventType: string
  actorDisplayName: string
  actorLogin: string | null
  summary: string
  targetType: string | null       // "ORDER" | "PRODUCTION_TASK" | null
  targetId: string | null         // UUID as string
}

export interface AuditLogPageResponse {
  items: AuditLogRowResponse[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface AuditLogFilters {
  from?: string                   // ISO-8601 date or datetime
  to?: string
  category?: AuditCategory[]      // multi-select
  actorUserId?: string
  search?: string
  page?: number
  size?: number
}

export interface UserSummaryResponse {
  id: string
  login: string
  displayName: string
}
```

## Composable: useAuditLog

```typescript
// frontend/cabinet/src/api/composables/use-audit-log.ts

interface UseAuditLogResult {
  data: Ref<AuditLogPageResponse | null>
  loading: Ref<boolean>
  error: Ref<{ kind: 'forbidden' | 'error'; message?: string } | null>
  refetch: (filters?: AuditLogFilters) => Promise<void>
}

export function useAuditLog(): UseAuditLogResult
```

### Behavior

- On first call / `refetch()`: `GET /api/audit` with filter params via `httpClient`
- Default filters: `{ from: 7 days ago ISO, to: now ISO, page: 0, size: 50 }`
- Uses `AbortController` — cancels in-flight request on new `refetch()` or scope dispose
- `CanceledError` is swallowed (no error state)
- HTTP 403 → `error.value = { kind: 'forbidden' }`
- Other HTTP errors → `error.value = { kind: 'error', message: ... }`
- Success → `data.value = response`, `error.value = null`
- `loading` is `true` while request is in-flight

### Parameter Mapping

| Filter field | Query param | Notes |
|-------------|-------------|-------|
| from | from | Sent as-is |
| to | to | Sent as-is |
| category | category (repeated) | `?category=AUTH&category=ORDER` |
| actorUserId | actorUserId | UUID string |
| search | search | Trimmed, omitted if empty |
| page | page | Default 0 |
| size | size | Default 50 |

## Composable: useUsersSearch

```typescript
// frontend/cabinet/src/api/composables/use-users-search.ts

export async function fetchUsers(
  search?: string,
  limit?: number,
): Promise<UserSummaryResponse[]>
```

### Behavior

- `GET /api/users?search={search}&limit={limit}` via `httpClient`
- Returns the array directly
- Throws on error (caller handles)

## Page: AuditLogPage.vue

```
Path: frontend/cabinet/src/pages/audit/AuditLogPage.vue
Route: /cabinet/audit
Route name: audit.list
Meta: { roles: ['ADMIN'], title: 'meta.title.audit' }
```

### Layout

```
┌──────────────────────────────────────────────────┐
│ Журнал действий                       [Обновить] │
├──────────────────────────────────────────────────┤
│ Filters:                                         │
│ [Дата с ___] [Дата по ___]                      │
│ [Категории: multi-select]  [Исполнитель: picker] │
│ [🔍 Поиск ___________________]    [Сбросить]    │
├──────────────────────────────────────────────────┤
│ Время │ Категория │ Событие │ Кто │ Описание │→ │
│───────┼───────────┼─────────┼─────┼──────────┼──│
│ 10:15 │ ORDER     │ CREATED │ adm │ Создан…  │🔗│
│ 10:14 │ AUTH      │ LOGIN   │ adm │ Вход…    │  │
│ 10:10 │ PROD_TASK │ STATUS  │ sup │ Статус…  │🔗│
├──────────────────────────────────────────────────┤
│         ◀ Назад  Стр. 1 из 3  Вперёд ▶          │
│                Всего: 142 событий                │
└──────────────────────────────────────────────────┘
```

### States

| State | Condition | Rendering |
|-------|-----------|-----------|
| Loading | `data === null && loading` | Skeleton rows |
| Empty | `data.items.length === 0` | «Событий за выбранный период нет» / «Под фильтры ничего не подходит — измените или сбросьте» |
| Error | `error.kind === 'error'` | Error banner + «Обновить» button |
| Forbidden | `error.kind === 'forbidden'` | Forbidden empty state, no data shown |
| Data | `data.items.length > 0` | Table + pagination |

### Filter Panel Behavior

- Date range: two date inputs (`from`, `to`); `from > to` → disable submit or show validation
- Category multi-select: checkboxes for AUTH / ORDER / PRODUCTION_TASK; default all checked
- Actor picker: `AuditActorPicker` component using `fetchUsers()`; shows displayName + login
- Search: text input, debounced 300ms
- Any filter change → `page` resets to 0 → `refetch()`
- «Сбросить»: resets all filters to defaults (last 7 days, all categories, no actor, no search, page 0)

### Table Columns

| Column | Content | Notes |
|--------|---------|-------|
| Время | `occurredAt` formatted as `dd.MM.yyyy HH:mm` | |
| Категория | Category badge (AUTH / ORDER / PRODUCTION_TASK) | Colored label |
| Событие | `eventType` | |
| Кто | `actorDisplayName` | |
| Описание | `summary` | Truncated if long |
| → | RouterLink icon | Only for ORDER → `orders.detail`, PRODUCTION_TASK → `production-tasks.detail`; absent for AUTH |

### Target Link Routing

```typescript
function targetRoute(row: AuditLogRowResponse) {
  if (row.targetType === 'ORDER' && row.targetId)
    return { name: 'orders.detail', params: { id: row.targetId } }
  if (row.targetType === 'PRODUCTION_TASK' && row.targetId)
    return { name: 'production-tasks.detail', params: { id: row.targetId } }
  return null
}
```

### Pagination

- Page controls: «Назад», «Вперёд», page number indicator
- `totalItems` and `totalPages` from response
- Page change → `refetch()` with new page number

### Tablet Adaptation (FR-019)

- Filter panel: wraps or collapses on narrow viewports (< 1024px)
- Table: `overflow-x-auto` with `min-w-` on columns for horizontal scroll
- All controls remain accessible on 10-12" landscape

## Component: AuditActorPicker.vue

```
Path: frontend/cabinet/src/components/domain/AuditActorPicker.vue
```

### Props & Events

```typescript
defineProps<{
  modelValue: string | null    // selected user ID
  disabled?: boolean
}>()
defineEmits<{
  'update:modelValue': [value: string | null]
}>()
```

### Behavior

- Text input with debounced search (300ms)
- Calls `fetchUsers(search, 30)` on input
- Dropdown shows matching users: `displayName (login)`
- Click to select; emits `update:modelValue` with user ID
- Clear button to deselect (emits null)
- Follows `ProductionTaskAssigneePicker` pattern

## Router Configuration

```typescript
// Added to frontend/cabinet/src/router/index.ts
{
  path: 'audit',
  name: 'audit.list',
  component: () => import('@/pages/audit/AuditLogPage.vue'),
  meta: {
    roles: ['ADMIN'],
    title: 'meta.title.audit',
  },
}
```

## Navigation Entry

```typescript
// Added to Sidebar.vue navigation items
{
  to: '/cabinet/audit',
  icon: ScrollText,  // from lucide-vue-next
  key: 'nav.audit',
  visible: permissions.value.isAdmin,
}
```

Placed after production tasks entries, visible only for ADMIN.

## I18n Keys

```typescript
// ru.ts additions
meta: {
  title: {
    audit: 'Журнал действий',
  },
},
nav: {
  audit: 'Журнал действий',
},
audit: {
  refresh: 'Обновить',
  resetFilters: 'Сбросить',
  filters: {
    dateFrom: 'Дата с',
    dateTo: 'Дата по',
    category: 'Категория',
    actor: 'Исполнитель',
    search: 'Поиск по описанию, номеру объекта или логину',
  },
  columns: {
    time: 'Время',
    category: 'Категория',
    eventType: 'Событие',
    actor: 'Кто',
    summary: 'Описание',
    target: '',
  },
  category: {
    AUTH: 'Авторизация',
    ORDER: 'Заказы',
    PRODUCTION_TASK: 'Производство',
  },
  empty: 'Событий за выбранный период нет',
  emptyFiltered: 'Под фильтры ничего не подходит — измените или сбросьте',
  errorLoading: 'Не удалось загрузить журнал',
  forbidden: 'Нет доступа к журналу действий',
  totalItems: 'Всего: {count} событий',
  page: 'Стр. {current} из {total}',
}
```

## Test Requirements

### Composable Tests (`use-audit-log.test.ts`)

- Default request: `GET /api/audit` with from/to (7 days) and page=0, size=50
- Filter params forwarded correctly (category repeated, search trimmed)
- 403 response → `error.kind = 'forbidden'`, data null
- Network error → `error.kind = 'error'`
- AbortController cancels prior request on re-fetch
- Scope dispose aborts in-flight request

### Page Tests (`AuditLogPage.test.ts`)

- Renders table with all columns when data present
- Loading state shows skeleton
- Empty state with appropriate message (no data vs filtered-out)
- Error banner with refresh button on error
- Forbidden state on 403
- RouterLink for ORDER and PRODUCTION_TASK rows, no link for AUTH rows
- Pagination controls update page and call refetch
- Refresh button calls refetch

### Filter Tests (`AuditLogPageFilters.test.ts`)

- Date range inputs bound to filter state
- Category multi-select toggles
- Actor picker emits filter change
- Search input debounced (300ms)
- Any filter change resets page to 0
- Reset button clears all filters to defaults

### Router Guard Tests (extend `guard.test.ts`)

- ADMIN → `audit.list` (allowed)
- isAdmin=true bypass → `audit.list` (allowed)
- ORDER_MANAGER → `forbidden`
- PRODUCTION_SUPERVISOR → `forbidden`
- PRODUCTION_EXECUTOR → `forbidden`
- Unauthenticated → `login`

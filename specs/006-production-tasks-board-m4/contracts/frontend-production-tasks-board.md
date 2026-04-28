# Contract: Production Tasks Board (Frontend)

This feature has **no new backend contract**. It consumes `GET /api/production-tasks` exactly as documented in `specs/005-production-tasks/contracts/production-tasks-api.md`. This file documents the frontend contract only.

## Route

| Path | Name | Component | Roles allowed (route meta) |
|---|---|---|---|
| `/cabinet/production-tasks/board` | `production-tasks.board` | `frontend/cabinet/src/pages/production/ProductionTasksBoardPage.vue` | `Order Manager`, `Shop Supervisor`, `Executor`, `ORDER_MANAGER`, `PRODUCTION_SUPERVISOR`, `PRODUCTION_EXECUTOR` (same allowlist as `production-tasks.list`) |

`meta.title` MUST resolve to `meta.title.productionTasks.board` (new i18n key, both `ru` and `en` locales). `meta.showBackButton` is **not** set; the board is a top-level navigation target. Roles outside this allowlist receive `/cabinet/403` (existing forbidden page).

## Composable

```ts
// frontend/cabinet/src/api/composables/use-production-tasks-board.ts

import type { Ref } from 'vue'
import type {
  ProductionTaskListFilters,
  ProductionTaskListRowResponse,
  ProductionTaskStatus,
} from '@/api/types/production-tasks'

export interface ProductionTasksBoardData {
  byStatus: Record<ProductionTaskStatus, ProductionTaskListRowResponse[]>
  totalVisible: number
  truncated: boolean
}

export interface UseProductionTasksBoardResult {
  data: Ref<ProductionTasksBoardData | null>
  loading: Ref<boolean>
  error: Ref<{ kind: string, message: string } | null>
  refetch: (filters?: ProductionTaskListFilters) => Promise<void>
}

export function useProductionTasksBoard(): UseProductionTasksBoardResult
```

Behavior:

- Issues `httpClient.get('/api/production-tasks', { params: { size: 200, ...filters } })`.
- Groups the response items into `data.byStatus` covering all four status keys (empty arrays when no items).
- Applies the COMPLETED-window cap (`updatedAt >= now − 7d`, sorted desc, take 30).
- When the active filter set carries an `overdueOnly` flag (UI-only), filters out non-overdue items client-side before grouping.
- Sets `data.truncated` to `response.totalItems > 200`.
- On 403 from the API: `error.value = { kind: 'forbidden', message: ... }` and `data.value = null`.
- On any other non-cancel error: `error.value = toApiError(e)`.
- Aborts in-flight requests on `refetch` and on scope dispose.

The composable MUST NOT add a client-side trust check on roles; it relies on server-side visibility from the existing list endpoint.

## Page

`ProductionTasksBoardPage.vue` MUST:

1. Render an `h1` titled by `t('meta.title.productionTasks.board')`.
2. Render filter controls: `search` (text), `executor` (existing assignee picker), `dueDateFrom`/`dueDateTo` (date inputs), `overdueOnly` (checkbox).
3. Render a manual «Обновить» button that calls `refetch(filters)`.
4. While loading and `data == null`: render four column skeleton placeholders.
5. When `error.value?.kind === 'forbidden'`: render the same forbidden empty state shape used by `ProductionTaskDetailPage` (amber banner + button «К списку задач»).
6. When `data.truncated`: render a non-blocking notice above the columns: «Показаны первые 200 задач — уточните фильтры, чтобы увидеть остальные».
7. Render four column components in the order: NOT_STARTED, IN_PROGRESS, BLOCKED, COMPLETED, each labelled by the Russian status label, each emitting `data.byStatus[status]` into `ProductionTaskBoardCard` instances.
8. Each card MUST link to `{ name: 'production-tasks.detail', params: { id } }` via `RouterLink` so middle-click and right-click open in new tab work.
9. On viewports `< lg` (Tailwind `< 1024px`), the column row MUST have `overflow-x-auto` and each column `min-w-[18rem]` for horizontal scroll.

## Component: `ProductionTaskBoardCard`

`frontend/cabinet/src/components/domain/ProductionTaskBoardCard.vue`. Props:

```ts
defineProps<{ row: ProductionTaskListRowResponse }>()
```

Render:

- Top line: `taskNumber` (font-mono, semibold).
- For status BLOCKED: truncated `blockedReason` directly under the task number, max 2 lines (Tailwind `line-clamp-2` / equivalent).
- Body line: `purpose` (one line, truncated).
- Context line: `order.orderNumber · order.customerDisplayName`.
- Footer row: executor display name (or «не назначен») on the left; `plannedFinishDate` formatted with `date-fns` + `ru` locale on the right.
- Overdue badge: when overdue rule matches, the date is rendered with the danger token and the word «просрочено», identical to the list view.

The card MUST emit no events; navigation happens via `RouterLink` wrapping it.

## Navigation entry

The cabinet's primary nav (existing layout component under `frontend/cabinet/src/components/layout/`) MUST gain a new item named «Доска» that targets `production-tasks.board`. The existing «Список задач» entry stays. Both items MUST be visible to all four allowed roles. Item order: list first, then board.

## Role guard expectations

Router guard (`frontend/cabinet/src/router/index.ts`) MUST:

- Apply the same allowlist + admin bypass behavior as the list route.
- Return `production-tasks.board` for an authenticated user holding any of the allowed roles.
- Return `forbidden` for any other authenticated user.
- Return `login` (with `from` parameter) for any unauthenticated user.

These cases MUST be covered in `tests/unit/router/guard.test.ts`.

## Negative behavior

- Token expires mid-session → cabinet's existing 401-interceptor logs the user out (no board-specific handling).
- API returns 5xx → page renders the existing error banner with a retry hint.
- API request is aborted (e.g., user navigates away) → composable swallows `CanceledError` and does not set `error`.

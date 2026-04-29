# Implementation Plan: Фронтенд уведомлений

**Branch**: `010-notification-frontend` | **Date**: 2026-04-29 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/010-notification-frontend/spec.md`

## Summary

Add notification UI to the Vue 3 SPA cabinet: bell icon with unread badge in TopBar (polling every 30s), dropdown with latest notifications and mark-all-read, full notifications page with pagination and filters, sidebar navigation item. Requires minimal backend change: add `targetEntityId` field to Notification entity/DTO for frontend routing.

## Technical Context

**Language/Version**: TypeScript 5.x (strict), Vue 3 (Composition API)
**Primary Dependencies**: Vue 3, Pinia, vue-router, vue-i18n, axios, lucide-vue-next, Tailwind CSS
**Storage**: Backend PostgreSQL via REST API (no frontend storage)
**Testing**: vitest + source-text TDD (readFileSync + string assertions)
**Target Platform**: Browser SPA (desktop + responsive)
**Project Type**: Web application frontend (SPA)
**Performance Goals**: Badge polling every 30s, dropdown loads < 500ms perceived
**Constraints**: No WebSocket/SSE (polling only), no new npm dependencies
**Scale/Scope**: ~15 files changed/created across frontend + ~5 backend files for targetEntityId

## Constitution Check

- **ERP domain fit**: Notifications close the feedback loop — backend triggers generate events on production task assignment, status change, overdue detection; frontend delivers them to the user for timely action. This directly strengthens the production control workflow.

- **Constraint-aware operations**: Overdue and blocked-status notifications are direct signals of flow problems. The UI enables faster human response, reducing waiting time. No domain facts are lost — notifications are read-only reflections of events already audited in the production module.

- **Architecture boundaries**: Frontend components handle display and navigation only. No business rules in Vue components — the backend determines who receives what notification. The Pinia store manages UI state (unread count, dropdown cache); all data comes from backend API. Backend change (targetEntityId) stays within notification domain entity, not leaking to production module.

- **Traceability/audit**: Notifications themselves are not audit events. Read/unread state is tracked in the notification entity. All underlying business events (task assignment, status changes) are already audited by the production module.

- **API-only/security**: All notification endpoints require JWT Bearer auth. The backend filters by `recipientUserId` from JWT — users see only their own notifications. Frontend adds Bearer token via axios interceptor (existing infrastructure). No new auth/role logic needed — notifications are visible to all authenticated users.

- **Docker/verifiability**: `make docker-up-detached && make health` proves stack starts. `pnpm typecheck && pnpm test && pnpm build` proves frontend correctness. Manual smoke test per quickstart.md. Backend change verified by `make backend-test-docker`.

- **Exception handling**: No constitution violations.

## Project Structure

### Documentation (this feature)

```text
specs/010-notification-frontend/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 research decisions
├── data-model.md        # Frontend types + backend extension
├── quickstart.md        # Manual verification steps
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code — Backend changes

```text
src/main/kotlin/com/ctfind/productioncontrol/
├── notifications/
│   ├── domain/
│   │   └── Notification.kt                    # + targetEntityId field
│   ├── application/
│   │   └── NotificationPorts.kt               # + targetEntityId in CreateNotificationCommand
│   └── adapter/
│       ├── persistence/
│       │   ├── NotificationJpaEntities.kt      # + targetEntityId column mapping
│       │   └── NotificationPersistenceAdapter.kt  # map field
│       └── web/
│           └── NotificationDtos.kt             # + targetEntityId in response
├── production/
│   └── application/
│       ├── AssignProductionTaskUseCase.kt       # pass task.id as targetEntityId
│       ├── ChangeProductionTaskStatusUseCase.kt # pass task.id as targetEntityId
│       └── OverdueTaskNotificationJob.kt        # pass task.id as targetEntityId
src/main/resources/db/migration/
└── V10__add_notification_target_entity_id.sql   # ADD COLUMN target_entity_id UUID
```

### Source Code — Frontend changes

```text
frontend/cabinet/src/
├── api/
│   ├── composables/
│   │   └── use-notifications.ts         # NEW: composable for notifications page
│   └── types/
│       └── notifications.ts             # NEW: notification types
├── components/
│   ├── layout/
│   │   ├── TopBar.vue                   # MODIFY: add NotificationBell component
│   │   └── Sidebar.vue                  # MODIFY: add "Уведомления" nav item
│   └── domain/
│       └── notifications/
│           ├── NotificationBell.vue     # NEW: bell icon + badge + dropdown
│           ├── NotificationDropdown.vue # NEW: dropdown content (list + actions)
│           └── NotificationItem.vue    # NEW: single notification row
├── pages/
│   └── notifications/
│       └── NotificationsPage.vue        # NEW: full page with pagination
├── stores/
│   └── notifications.ts                 # NEW: Pinia store for unread count + polling
├── router/
│   └── index.ts                         # MODIFY: add /cabinet/notifications route
├── i18n/
│   ├── keys.ts                          # MODIFY: add notifications namespace
│   └── ru.ts                            # MODIFY: add Russian translations
└── utils/
    └── relative-time.ts                 # NEW: relative time formatter

frontend/cabinet/tests/unit/
├── composables/
│   └── use-notifications.test.ts        # NEW: composable tests
├── stores/
│   └── notifications.test.ts            # NEW: store tests
├── pages/
│   └── NotificationsPage.test.ts        # NEW: page tests
└── components/
    └── NotificationBell.test.ts         # NEW: bell + dropdown tests
```

**Structure Decision**: Frontend follows existing cabinet SPA layout. New notification components live in `components/domain/notifications/` following the established domain component pattern. Pinia store for global state (unread count). Composable for page-scoped list state.

## Key Implementation Patterns

### Pinia store: `useNotificationStore`

```typescript
// stores/notifications.ts
export const useNotificationStore = defineStore('notifications', () => {
  const unreadCount = ref(0)
  const dropdownItems = ref<NotificationResponse[] | null>(null)
  const dropdownLoading = ref(false)
  let pollTimer: ReturnType<typeof setInterval> | null = null

  async function fetchUnreadCount() { /* GET /api/notifications/unread-count */ }
  async function fetchDropdown() { /* GET /api/notifications?size=10 */ }
  async function markRead(id: string) { /* PATCH /api/notifications/{id}/read */ }
  async function markAllRead() { /* POST /api/notifications/mark-all-read */ }
  function startPolling() { /* setInterval 30s + visibilitychange */ }
  function stopPolling() { /* clearInterval */ }

  return { unreadCount, dropdownItems, dropdownLoading,
           fetchUnreadCount, fetchDropdown, markRead, markAllRead,
           startPolling, stopPolling }
})
```

### Composable: `useNotifications`

Follows `useAuditLog` pattern — returns `{ data, loading, error, refetch }` for paginated list.

### Navigation helper

```typescript
function notificationRoute(n: NotificationResponse): RouteLocationRaw | null {
  if (!n.targetType || !n.targetEntityId) return null
  switch (n.targetType) {
    case 'PRODUCTION_TASK': return { name: 'production-tasks.detail', params: { id: n.targetEntityId } }
    case 'ORDER': return { name: 'orders.detail', params: { id: n.targetEntityId } }
    default: return null
  }
}
```

### Relative time helper

```typescript
function relativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  if (diff < 60_000) return 'только что'
  if (diff < 3600_000) return `${Math.floor(diff / 60_000)} мин назад`
  if (diff < 86400_000) return `${Math.floor(diff / 3600_000)} ч назад`
  if (diff < 172800_000) return 'вчера'
  if (diff < 604800_000) return `${Math.floor(diff / 86400_000)} дн назад`
  return new Date(iso).toLocaleDateString('ru-RU')
}
```

### Notification type icons (lucide-vue-next)

| Type | Icon | Color class |
|------|------|-------------|
| TASK_ASSIGNED | `UserPlus` | `text-blue-500` |
| STATUS_CHANGED | `ArrowRightLeft` | `text-amber-500` |
| TASK_OVERDUE | `AlertTriangle` | `text-red-500` |

### Polling lifecycle

- Start polling when user logs in (in `AppShell` or router guard `afterEach`)
- Pause on `document.visibilityState === 'hidden'`
- Resume on `document.visibilityState === 'visible'` (immediate fetch + restart interval)
- Stop on logout

## Complexity Tracking

No constitution violations. No complexity exceptions needed.

# Implementation Plan: Инфраструктура и модель внутренних уведомлений

**Branch**: `008-notifications-infrastructure` | **Date**: 2026-04-28 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/008-notifications-infrastructure/spec.md`

## Summary

New `notifications` backend module (hexagonal: domain/application/adapter) providing the Notification entity, Flyway migration, internal creation port for cross-module use, and REST API for authenticated users to list, filter, and manage read-state of their own notifications. This is the foundational infrastructure layer for M7 (Internal Notifications); triggering logic (spec 2) and frontend (spec 3) build on top.

## Technical Context

**Language/Version**: Kotlin / Java 21, Spring Boot 3.x
**Primary Dependencies**: Spring Web, Spring Security (JWT Bearer), Spring Data JPA, Flyway
**Storage**: PostgreSQL (existing instance, port 15432 local)
**Testing**: JUnit 5, MockMvc, Testcontainers (for persistence tests)
**Target Platform**: Linux server (Docker Compose)
**Project Type**: Modular monolith backend module
**Performance Goals**: List page (20 items) < 1s at 10k notifications/user; unread-count optimized for 30s polling
**Constraints**: No WebSocket/SSE; sync-only creation port; no notification deletion; no preference settings
**Scale/Scope**: Up to 10k notifications per user; all authenticated roles

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **ERP domain fit**: Notifications strengthen the operational model by providing an information delivery channel between production tasks, orders, and users. This is the M7 milestone entity explicitly listed in the constitution's Domain-First ERP Core principle.
- **Constraint-aware operations**: Notification preserves createdAt (when event happened) and readAt (when user reacted). The difference readAt − createdAt provides reaction time data for future TOC analysis. targetType/targetId link notifications to the task flow. No hardcoded priority model — notifications are sorted by createdAt DESC but this is a display choice, not a domain priority constraint.
- **Architecture boundaries**: Domain layer: Notification data class with validation (pure Kotlin, no Spring). Application layer: use cases (CreateNotificationUseCase, ListNotificationsUseCase, MarkReadUseCase) + ports. Controllers only adapt HTTP ↔ use cases. No business rules in controllers or DTOs.
- **Traceability/audit**: Notifications themselves serve as an audit trail of user information delivery. readAt captures when user acknowledged the notification. No deletion in Phase 1 ensures complete history.
- **API-only/security**: All endpoints return 401 for unauthenticated, data isolation via recipientUserId === JWT user. No form login, no browser challenges. Security config: `/api/notifications/**` under existing `.anyRequest().authenticated()` — no change needed.
- **Docker/verifiability**: New Flyway migration V6. Docker Compose startup unaffected (new module is pure backend, no new services). Verification: `make backend-test`, `make backend-build`, `make health`.
- **Exception handling**: No constitution violations identified.

## Project Structure

### Documentation (this feature)

```text
specs/008-notifications-infrastructure/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── notification-list.contract.md
│   ├── notification-unread-count.contract.md
│   ├── notification-mark-read.contract.md
│   └── notification-mark-all-read.contract.md
└── tasks.md             # Phase 2 output (via /speckit-tasks)
```

### Source Code (repository root)

```text
src/main/kotlin/com/ctfind/productioncontrol/
└── notifications/
    ├── domain/
    │   ├── Notification.kt              # Domain entity + validation
    │   ├── NotificationType.kt          # Enum (stored as VARCHAR)
    │   └── NotificationTargetType.kt    # Enum: ORDER, PRODUCTION_TASK
    ├── application/
    │   ├── NotificationPorts.kt         # NotificationPersistencePort, NotificationCreatePort
    │   ├── NotificationModels.kt        # Query, PageResult, DTOs between layers
    │   ├── CreateNotificationUseCase.kt # Internal creation (implements NotificationCreatePort)
    │   ├── ListNotificationsUseCase.kt  # List + unread count
    │   └── MarkNotificationReadUseCase.kt # Mark one + mark all
    └── adapter/
        ├── persistence/
        │   ├── NotificationJpaEntities.kt
        │   ├── NotificationJpaRepositories.kt
        │   └── NotificationPersistenceAdapter.kt
        └── web/
            ├── NotificationController.kt
            └── NotificationDtos.kt

src/main/resources/db/migration/
└── V6__create_notification_table.sql

src/test/kotlin/com/ctfind/productioncontrol/notifications/
├── domain/
│   └── NotificationTests.kt            # Domain validation tests
├── application/
│   ├── CreateNotificationUseCaseTests.kt
│   ├── ListNotificationsUseCaseTests.kt
│   └── MarkNotificationReadUseCaseTests.kt
└── adapter/
    ├── persistence/
    │   └── NotificationPersistenceAdapterTests.kt
    └── web/
        ├── NotificationControllerListTests.kt
        ├── NotificationControllerReadTests.kt
        └── NotificationControllerSecurityTests.kt
```

**Structure Decision**: New `notifications` module follows the same hexagonal layout as `production`, `orders`, `auth`, and `audit` modules. Domain is pure Kotlin with no framework dependencies. Application owns use cases and ports. Adapters handle HTTP and JPA mapping.

## Complexity Tracking

No constitution violations. No complexity exceptions needed.

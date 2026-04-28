# Research: 008-notifications-infrastructure

## R1: NotificationType storage strategy (VARCHAR vs Postgres enum)

**Decision**: Store NotificationType as VARCHAR in PostgreSQL, use Kotlin enum in domain layer.

**Rationale**: Spec requirement FR-010 mandates adding new types without schema changes. Postgres `CREATE TYPE` enums require `ALTER TYPE ... ADD VALUE` migrations for each new value. VARCHAR allows new Kotlin enum values to be persisted immediately. JPA `@Enumerated(EnumType.STRING)` handles this naturally. The existing `ProductionTaskStatus` uses `EnumType.STRING` — same pattern.

**Alternatives considered**:
- Postgres native enum: Rejected — requires migration per new type, violates FR-010.
- Integer codes: Rejected — less readable in raw SQL queries, no benefit over VARCHAR for this scale.

## R2: User ID extraction from JWT

**Decision**: Reuse the existing `Jwt.claims["userId"]` extraction pattern from `ProductionTaskDtos.kt`.

**Rationale**: The production module already extracts `userId: UUID` from JWT claims via `(claims["userId"] as? String)?.let(UUID::fromString)` with a deterministic fallback `UUID.nameUUIDFromBytes(subject.toByteArray())`. The notification controller will use the same pattern to identify the current user.

**Alternatives considered**:
- New shared utility: Rejected for now — only two consumers (production, notifications). Extract to shared if a third module needs it.
- Spring Security principal object: The project uses raw JWT, not a custom UserDetails.

## R3: Pagination approach

**Decision**: Use Spring Data `Pageable` / `Page<T>` at the JPA layer, map to a custom `NotificationPageResult` at the application layer (matching the `ProductionTaskPageResult` pattern).

**Rationale**: Spring Data's `Page<T>` provides totalItems and totalPages. The application layer wraps this in a framework-free model. The controller maps to a response DTO with `items`, `page`, `size`, `totalItems`, `totalPages`.

**Alternatives considered**:
- Cursor-based pagination: Spec explicitly defers infinite scroll / cursor — out of scope.
- Raw SQL `LIMIT/OFFSET`: Unnecessary when Spring Data handles it.

## R4: mark-all-read implementation

**Decision**: Use a JPA `@Modifying @Query` bulk UPDATE with `WHERE recipient_user_id = :userId AND read = false`, returning affected row count.

**Rationale**: Single SQL statement is atomic and performant. No need to load entities into memory. The `readAt` field is set in the same UPDATE. Spring Data's `@Modifying` supports returning `Int` (affected rows).

**Alternatives considered**:
- Load all unread, iterate, save: Rejected — N+1 queries, poor performance at scale.
- Native query: Not needed — JPQL supports the required `UPDATE ... SET ... WHERE`.

## R5: Security config changes

**Decision**: No changes to SecurityConfig.kt needed.

**Rationale**: The existing config uses `.anyRequest().authenticated()` as the default — all `/api/notifications/**` endpoints are automatically JWT-protected. No role-based restrictions needed (all roles can access their own notifications). The data isolation is enforced at the use-case level (recipientUserId === JWT userId), not at the security config level.

## R6: readAt idempotency

**Decision**: On mark-read, set `readAt = Instant.now()` only when `read == false` (first read). If already read, leave readAt unchanged.

**Rationale**: Spec FR-006 explicitly states "повторная пометка НЕ ДОЛЖНА перезаписывать ранее зафиксированный момент". This preserves the first-reaction timestamp for TOC analysis. Implementation: `if (!notification.read) { notification.read = true; notification.readAt = now }`.

**Alternatives considered**:
- Always overwrite readAt: Rejected — violates spec, loses first-reaction data.
- Separate mark-read and mark-unread: Out of scope (spec has no "mark unread" operation).

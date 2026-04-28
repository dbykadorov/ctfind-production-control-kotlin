# Data Model: Журнал действий администратора (Phase 1 §8 #10)

**Feature**: 007-audit-log-viewer
**Date**: 2026-04-28

## Overview

This feature introduces **no new database tables or migrations**. It creates read-only view models that unify data from three existing audit tables into a single feed. The data model below describes the application-layer entities used by the audit module.

## Source Tables (existing, read-only)

### auth_audit_event (V2)

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| id | UUID | PK | |
| event_type | VARCHAR(60) | NOT NULL | LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, LOCAL_SEED |
| outcome | VARCHAR(80) | NOT NULL | SUCCESS, INVALID_CREDENTIALS, THROTTLED, etc. |
| login | VARCHAR(120) | YES | User login attempted |
| user_id | UUID | YES | FK → app_user(id) ON DELETE SET NULL |
| request_ip | VARCHAR(80) | YES | |
| user_agent | VARCHAR(500) | YES | |
| occurred_at | TIMESTAMPTZ | NOT NULL | Event timestamp |
| details | TEXT | YES | |

Indexes: `ix_auth_audit_event_occurred_at` (occurred_at), `ix_auth_audit_event_login` (login)

### order_audit_event (V4)

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| id | UUID | PK | |
| event_type | VARCHAR(60) | NOT NULL | |
| actor_user_id | UUID | NOT NULL | FK → app_user(id) |
| target_type | VARCHAR(80) | NOT NULL | e.g. ORDER |
| target_id | UUID | NOT NULL | Order UUID |
| event_at | TIMESTAMPTZ | NOT NULL | Event timestamp |
| summary | TEXT | NOT NULL | Human-readable description |
| metadata | TEXT | YES | JSON payload |

Indexes: `ix_order_audit_event_target` (target_type, target_id), `ix_order_audit_event_event_at` (event_at)

### production_task_audit_event (V5)

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| id | UUID | PK | |
| event_type | VARCHAR(128) | NOT NULL | |
| actor_user_id | UUID | NOT NULL | FK → app_user(id) |
| target_type | VARCHAR(64) | NOT NULL | e.g. PRODUCTION_TASK |
| target_id | UUID | NOT NULL | Task UUID |
| event_at | TIMESTAMPTZ | NOT NULL | Event timestamp |
| summary | VARCHAR(500) | NOT NULL | Human-readable description |
| metadata | TEXT | YES | JSON payload |

Indexes: `idx_production_task_audit_target` (target_type, target_id)

### app_user (V2, for actor resolution)

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| id | UUID | PK | |
| login | VARCHAR(120) | NOT NULL | UNIQUE |
| display_name | VARCHAR(200) | NOT NULL | |
| enabled | BOOLEAN | NOT NULL | |

## Application-Layer Entities (new)

### AuditCategory (enum)

```
AUTH | ORDER | PRODUCTION_TASK
```

Maps 1:1 to the source table of origin.

### AuditLogRow (domain view model)

Unified representation of one audit event from any of the three sources.

| Field | Type | Source mapping |
|-------|------|---------------|
| id | UUID | PK from source table |
| occurredAt | Instant | `occurred_at` (auth) or `event_at` (order/production) |
| category | AuditCategory | Constant per source table |
| eventType | String | `event_type` from source |
| actorUserId | UUID? | `user_id` (auth, nullable) or `actor_user_id` (order/production) |
| actorDisplayName | String | JOIN app_user.display_name; fallback: login → «Удалённый пользователь» |
| actorLogin | String? | `login` (auth) or JOIN app_user.login |
| summary | String | `summary` (order/production) or generated from event_type+outcome+login (auth) |
| targetType | String? | `target_type` (order/production) or null (auth) |
| targetId | UUID? | `target_id` (order/production) or null (auth) |

### AuditLogQuery (query model)

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| from | Instant | 7 days ago | Inclusive lower bound |
| to | Instant | now | Exclusive upper bound |
| categories | Set\<AuditCategory\>? | null (= all) | Filter by source |
| actorUserId | UUID? | null | Filter by actor |
| search | String? | null | ILIKE on summary + targetId + actorLogin |
| page | Int | 0 | Zero-based page index |
| size | Int | 50 | Page size |

### AuditLogPageResult

| Field | Type | Notes |
|-------|------|-------|
| items | List\<AuditLogRow\> | Current page |
| page | Int | Zero-based index |
| size | Int | Requested page size |
| totalItems | Long | Total matching events across all pages |
| totalPages | Int | Computed: ceil(totalItems / size) |

### UserSummary (for actor picker)

| Field | Type | Source |
|-------|------|--------|
| id | UUID | app_user.id |
| login | String | app_user.login |
| displayName | String | app_user.display_name |

## Column Normalization Map

| Unified Field | auth_audit_event | order_audit_event | production_task_audit_event |
|---------------|------------------|-------------------|-----------------------------|
| id | id | id | id |
| occurredAt | occurred_at | event_at | event_at |
| category | `AUTH` (const) | `ORDER` (const) | `PRODUCTION_TASK` (const) |
| eventType | event_type | event_type | event_type |
| actorUserId | user_id (nullable) | actor_user_id | actor_user_id |
| actorLogin | login | JOIN app_user | JOIN app_user |
| actorDisplayName | JOIN app_user → login fallback | JOIN app_user | JOIN app_user |
| summary | generated | summary | summary |
| targetType | null | target_type | target_type |
| targetId | null | target_id | target_id |

## Relationships

```
AuditLogRow ──reads──▶ auth_audit_event
AuditLogRow ──reads──▶ order_audit_event
AuditLogRow ──reads──▶ production_task_audit_event
All three   ──joins──▶ app_user (for actor display name / login)
```

No new foreign keys, no new tables, no migrations.

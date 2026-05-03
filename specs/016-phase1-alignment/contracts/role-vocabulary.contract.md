# Contract: Phase 1 Role Vocabulary

## Canonical Backend Role Codes

Current access behavior MUST use only these backend role codes:

| Code | Human Label | Phase 1 Responsibility |
|------|-------------|------------------------|
| `ADMIN` | Administrator | Administration, users, audit, full operational access |
| `ORDER_MANAGER` | Order Manager | Orders, customers, order planning, BOM editing where applicable |
| `WAREHOUSE` | Warehouse | Materials, stock receipts, stock consumption |
| `PRODUCTION_SUPERVISOR` | Production Supervisor | Production task planning, assignment, board/control views |
| `PRODUCTION_EXECUTOR` | Production Executor | Assigned production task execution |

## Rules

- Requirements and contracts MUST NOT use `WAREHOUSE_MANAGER` as a current backend role code.
- Human-facing labels MAY appear in prose, but access rules MUST map them to canonical role codes.
- Historical labels MUST be qualified as historical or replaced.
- This alignment MUST NOT add, remove, or weaken permissions.

## Validation Search

Reviewers should search Phase 1 docs/specs for:

```text
WAREHOUSE_MANAGER
Order Corrector
Shop Supervisor
Executor
Warehouse
Administrator
```

Any result that defines access behavior must either use a canonical code or explicitly map the label to one.

# Contract: Phase 1 Acceptance Baseline

## Required Acceptance Areas

The Phase 1 completion criteria must let a reviewer verify these areas:

| Area | Required Acceptance Outcome |
|------|-----------------------------|
| Administrator bootstrap | Local and production-like administrator bootstrap is documented and verified or explicitly deferred |
| Users and roles | Admin can create/edit users and assign canonical Phase 1 roles |
| Orders | Order creation, editing, status movement, list/detail visibility, and customer context are covered |
| Production tasks | Orders can be split into tasks, assigned, planned, and moved through task statuses |
| Work interfaces | Task board and executor task views expose current work according to role visibility |
| Execution control | Overdue and current status visibility are covered for orders and tasks |
| Audit log | Relevant business-state changes are visible in a reviewable audit log or explicitly deferred |
| Internal notifications | Assignment, status-change, and overdue notifications are covered or explicitly deferred |
| Warehouse materials | Materials, receipts, and current stock visibility are covered |
| Stock consumption | Material consumption under orders is covered or explicitly deferred with sign-off impact |

## Rules

- A capability listed in the Phase 1 scope must not disappear from completion criteria.
- A deferred area must include reason, owner, and sign-off impact.
- Acceptance wording should be business-facing and demonstrable without reading source code.
- This contract does not change the product scope; it aligns documented acceptance with existing Phase 1 scope.

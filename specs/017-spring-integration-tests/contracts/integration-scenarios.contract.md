# Contract: Phase 1 Integration Scenarios

## MVP Scenario Set

The MVP integration suite must include these scenarios:

| Scenario | Priority | Primary Flow | Required Negative Checks |
|----------|----------|--------------|--------------------------|
| `AuthUsersSecurityIntegrationTest` | P1 | Bootstrap admin, login, current-user, users API | non-admin users API rejection; unauthenticated protected access |
| `ProductionTaskLifecycleIntegrationTest` | P1 | order -> production task -> assignment -> status lifecycle -> history | executor cannot access another user's task; stale version conflict; unauthenticated access |
| `WarehouseConsumptionIntegrationTest` | P1 | material -> receipt -> order BOM -> consumption -> usage | insufficient stock rejection; shipped-order rejection; unauthorized role rejection |

Implementation status: complete. The MVP contains exactly these three scenario classes.

## Follow-Up Scenario Set

The full planned suite should include these scenarios:

| Scenario | Priority | Primary Flow | Required Negative Checks |
|----------|----------|--------------|--------------------------|
| `OrderLifecycleIntegrationTest` | P2 | create/list/detail/update/status lifecycle/shipped restriction | non-writer rejection; invalid transition rejection |
| `NotificationsIntegrationTest` | P2 | assignment/status/overdue notifications and read state | user isolation; duplicate overdue guard |
| `AuditFeedIntegrationTest` | P2 | auth/order/production/inventory events in admin feed | non-admin rejection; unauthenticated rejection; filter behavior |

Implementation status: complete. The full Phase 1 suite contains six scenario classes total.

## Rules

- Scenario classes must be organized by business flow, not by controller class.
- Each scenario must use the real application request path for behavior under verification.
- Each scenario must keep negative checks focused; broad business-rule matrices remain in unit tests.
- Each scenario must include a short coverage/residual-risk note in code comments, test display names, or feature documentation.
- The suite must not exceed six Phase 1 scenario classes or equivalent scenario groups without a new spec update.

## Residual Risk Ownership

- Detailed business-rule permutations remain in existing unit/application tests.
- Integration scenarios intentionally cover one business happy path plus focused negative/security/conflict cases.
- Frontend smoke coverage remains outside this backend integration suite.
- All Phase 1 audit categories currently expected by the contract are represented by the implemented audit scenario.

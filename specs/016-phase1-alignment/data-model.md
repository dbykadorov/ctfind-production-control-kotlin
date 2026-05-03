# Data Model: Phase 1 Alignment

This feature models documentation and sign-off artifacts, not runtime database entities.

## Phase 1 Acceptance Baseline

Represents the capability set required before Phase 1 can be considered complete.

**Fields**

- `capability`: Human-readable business capability name.
- `scopeStatus`: `accepted`, `deferred`, or `out_of_scope`.
- `acceptanceOutcome`: Concrete condition that must be demonstrable.
- `sourceArtifact`: Document or spec that defines the capability.
- `evidenceReference`: Optional reference to verification evidence or sign-off record.

**Validation Rules**

- Every accepted capability must have an acceptance outcome.
- Every deferred capability must have a reason and sign-off impact.
- The baseline must cover administrator bootstrap, users and roles, orders, production tasks, board/executor workflow, overdue/status visibility, audit log, internal notifications, warehouse materials, and stock consumption under orders.

## Canonical Role Vocabulary

Represents the approved backend role codes and their relationship to human-facing labels.

**Fields**

- `roleCode`: One of `ADMIN`, `ORDER_MANAGER`, `WAREHOUSE`, `PRODUCTION_SUPERVISOR`, `PRODUCTION_EXECUTOR`.
- `label`: Human-facing role name.
- `description`: Scope of responsibility in Phase 1.
- `legacyTerms`: Optional historical or incorrect terms that must not drive access decisions.

**Validation Rules**

- Requirements and contracts that define access behavior must use canonical role codes.
- Human labels are allowed only when access behavior is unambiguous.
- `WAREHOUSE_MANAGER` must not appear as a current backend role code.

## Verification Item

Represents a test, smoke check, build, health check, visual sign-off, or manual review needed for Phase 1 confidence.

**Fields**

- `artifact`: Spec or document containing the item.
- `item`: The verification activity.
- `disposition`: `passed`, `failed`, `blocked`, or `deferred`.
- `evidence`: Required for `passed`; optional but recommended for `failed`.
- `owner`: Required for `blocked` and `deferred`.
- `reason`: Required for `blocked` and `deferred`.
- `signOffImpact`: Required for `failed`, `blocked`, and `deferred`.
- `date`: Date of the disposition decision.

**Validation Rules**

- No open verification item may remain without disposition.
- Passed items must include evidence and date.
- Deferred items must explain whether Phase 1 can still be signed off.

## Specification Status

Represents the visible state of each Phase 1 spec.

**Fields**

- `spec`: Feature spec directory.
- `status`: `Accepted`, `Pending verification`, `Deferred`, `Blocked`, `Superseded`, or `Draft`.
- `statusReason`: Short explanation for non-accepted states.
- `evidenceReference`: Optional reference to tasks, quickstart, or sign-off evidence.

**Validation Rules**

- Implemented and accepted specs must not remain generic `Draft`.
- Specs with incomplete verification must use a status that exposes the gap.
- Status wording should be consistent across Phase 1 specs.

## Deferred Follow-up

Represents work or verification intentionally moved outside the Phase 1 acceptance path.

**Fields**

- `sourceItem`: Original task, check, or requirement.
- `owner`: Person or role responsible for follow-up.
- `reason`: Why it was deferred.
- `target`: Follow-up destination, such as a later spec, backlog item, or release checklist.
- `signOffImpact`: Whether Phase 1 can be signed off with this item deferred.

**Validation Rules**

- Deferred follow-ups must not be hidden in prose only; they need a reviewable record.
- A deferred follow-up must not change runtime scope silently.

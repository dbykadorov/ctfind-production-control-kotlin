# Feature Specification: Phase 1 Alignment

**Feature Branch**: `016-phase1-alignment`  
**Created**: 2026-05-03  
**Status**: Accepted  
**Input**: User description: "Выравнивание Phase 1: 1. Нормализовать role vocabulary в specs/contracts: WAREHOUSE, ADMIN, ORDER_MANAGER, PRODUCTION_SUPERVISOR, PRODUCTION_EXECUTOR. 2. Закрыть или явно перенести open verification items в 007 и 015. 3. Обновить docs/PHASE_01.md completion criteria под фактический scope фазы. 4. Проставить финальные статусы specs и привести 008/tasks.md к checkbox-формату."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Align Phase 1 Acceptance Scope (Priority: P1)

As a project owner preparing Phase 1 sign-off, I need the Phase 1 completion criteria to match the actual delivered scope so that acceptance decisions are based on the same baseline across documentation, specifications, and review notes.

**Why this priority**: Without a single acceptance baseline, Phase 1 can be marked complete while warehouse, notifications, audit verification, or user administration remain ambiguous.

**Independent Test**: Review the Phase 1 completion criteria and confirm that every delivered Phase 1 business capability has an explicit acceptance criterion or an explicit out-of-scope/deferred note.

**Acceptance Scenarios**:

1. **Given** Phase 1 includes orders, production tasks, warehouse, audit, notifications, and user administration, **When** the completion criteria are reviewed, **Then** each area is represented by a concrete acceptance outcome.
2. **Given** a Phase 1 capability is not intended to be accepted in this phase, **When** the completion criteria are reviewed, **Then** the capability is explicitly marked as deferred or out of scope with a clear reason.
3. **Given** a stakeholder reads only the Phase 1 document, **When** they decide whether Phase 1 is ready for sign-off, **Then** they can identify which capabilities must be demonstrated before sign-off.

---

### User Story 2 - Normalize Role Vocabulary (Priority: P1)

As a product and engineering team, we need all Phase 1 documentation and specifications to use the same canonical role vocabulary so that access expectations are consistent across requirements, contracts, tests, and UI behavior.

**Why this priority**: Role drift creates authorization ambiguity and can cause future changes to reintroduce conflicting constants or permissions.

**Independent Test**: Search Phase 1 documentation and specification artifacts for role references and confirm that canonical role codes are used consistently where backend role codes are required.

**Acceptance Scenarios**:

1. **Given** a requirement or contract references a backend role code, **When** role vocabulary is reviewed, **Then** it uses one of `ADMIN`, `ORDER_MANAGER`, `WAREHOUSE`, `PRODUCTION_SUPERVISOR`, or `PRODUCTION_EXECUTOR`.
2. **Given** a document uses a human-facing role label, **When** it is reviewed, **Then** the label is clearly mapped to a canonical role code where access behavior depends on it.
3. **Given** legacy or conflicting role names appear in existing artifacts, **When** this alignment is complete, **Then** they are replaced or explicitly documented as historical wording that must not drive access decisions.

---

### User Story 3 - Resolve Open Verification Evidence (Priority: P1)

As a release reviewer, I need open verification items in completed Phase 1 specifications either closed with fresh evidence or explicitly moved out of the Phase 1 sign-off path so that the release state is honest and auditable.

**Why this priority**: A phase cannot be treated as complete while required verification remains silently deferred.

**Independent Test**: Review Phase 1 task lists and sign-off records and confirm that no unchecked verification task remains without a recorded decision: passed, failed, blocked with owner, or deferred with scope impact.

**Acceptance Scenarios**:

1. **Given** a verification item is still required for Phase 1 sign-off, **When** it is reviewed, **Then** it has a fresh result and date.
2. **Given** a verification item cannot be completed before Phase 1 sign-off, **When** it is reviewed, **Then** it is explicitly moved to a follow-up backlog with owner, reason, and sign-off impact.
3. **Given** a manual visual or role smoke check is required, **When** it is reviewed, **Then** the sign-off artifact records whether it passed or remains a release risk.

---

### User Story 4 - Finalize Spec Status and Task Format (Priority: P2)

As a team member planning the next phase, I need Phase 1 specifications to show accurate final status and task completion format so that future planning does not depend on stale `Draft` states or non-trackable task lists.

**Why this priority**: Status and task-format cleanup does not change product behavior, but it protects traceability and reduces planning noise.

**Independent Test**: Review all Phase 1 spec status fields and task files and confirm that completed features have accurate final statuses and trackable task checkboxes.

**Acceptance Scenarios**:

1. **Given** a Phase 1 specification is implemented and accepted, **When** its status is reviewed, **Then** it is marked with a final status that reflects the actual state.
2. **Given** a Phase 1 specification has open verification or deferred work, **When** its status is reviewed, **Then** the status reflects that it is not fully accepted or references the deferred scope.
3. **Given** a task list is used for completion tracking, **When** it is reviewed, **Then** every actionable task has a checkbox state.

---

### Edge Cases

- What happens if a role appears only as a user-facing label? It may remain as a label if the artifact also makes the canonical access role clear where permissions are involved.
- What happens if a spec is implemented but lacks manual sign-off? Its final status must not imply full acceptance unless the missing sign-off is explicitly waived or moved out of Phase 1.
- What happens if verification evidence exists outside the spec folder? The relevant task or sign-off artifact must reference the evidence location and date.
- What happens if an existing historical spec intentionally uses old terminology? The artifact must clearly state that the old name is historical and must not be used as a current role code.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The alignment MUST define the canonical Phase 1 backend role vocabulary as `ADMIN`, `ORDER_MANAGER`, `WAREHOUSE`, `PRODUCTION_SUPERVISOR`, and `PRODUCTION_EXECUTOR`.
- **FR-002**: The alignment MUST remove or qualify conflicting role code names from Phase 1 specs and contracts where they describe current access behavior.
- **FR-003**: The alignment MUST preserve human-facing role labels only when they are clearly distinguishable from backend role codes.
- **FR-004**: The Phase 1 completion criteria MUST include all accepted Phase 1 business capabilities: administrator bootstrap, users and roles, orders, production tasks, task board/executor workflow, overdue/status visibility, audit log, internal notifications, warehouse materials, and stock consumption under orders.
- **FR-005**: The Phase 1 completion criteria MUST identify any capability that is intentionally deferred from Phase 1 acceptance.
- **FR-006**: Open verification items in the audit-log and dark-theme specifications MUST be closed with evidence or explicitly transferred to follow-up work with owner, reason, and sign-off impact.
- **FR-007**: Specification statuses for Phase 1 features MUST reflect their actual review state rather than leaving implemented work as generic draft.
- **FR-008**: The notifications infrastructure task list MUST use checkbox task states for every actionable task.
- **FR-009**: The alignment MUST leave a clear audit trail of what was changed in documentation and specification artifacts.
- **FR-010**: The alignment MUST not change product behavior, user permissions, data semantics, or runtime workflows unless a separate implementation feature explicitly requests it.

### Key Entities

- **Phase 1 Acceptance Baseline**: The set of business capabilities that must be demonstrable before Phase 1 is considered complete.
- **Canonical Role Vocabulary**: The approved backend role codes and their relationship to human-facing labels.
- **Verification Item**: A required test, smoke check, build, health check, or sign-off action that provides evidence for acceptance.
- **Specification Status**: The visible state of a feature specification, such as draft, ready, accepted, deferred, or blocked.
- **Deferred Follow-up**: Work or verification intentionally moved outside the Phase 1 acceptance path with explicit rationale and impact.

### Constitution Alignment *(mandatory)*

- **ERP/domain fit**: Strengthens the Phase 1 operational model by aligning orders, production tasks, inventory, roles, audit, users, and internal notifications under one acceptance baseline.
- **TOC readiness**: Preserves the documented facts required for later flow and constraint analysis by making task status, overdue visibility, audit, inventory usage, and verification scope explicit.
- **Traceability/audit**: Makes documentation and specification changes traceable by requiring explicit status, verification, and deferred-work records.
- **Security/API boundary**: Clarifies role vocabulary and permission expectations without changing authorization behavior or introducing new access rules.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A reviewer can identify the complete Phase 1 acceptance baseline from the Phase 1 document in under 10 minutes without cross-reading every feature specification.
- **SC-002**: A search across Phase 1 requirements and contracts finds no unqualified current role code outside the canonical vocabulary.
- **SC-003**: All Phase 1 task lists used for completion tracking have checkbox states for actionable tasks.
- **SC-004**: Every open verification item from the audit-log and dark-theme specifications is either marked complete with evidence or documented as deferred with owner and sign-off impact.
- **SC-005**: All Phase 1 specifications have statuses that distinguish accepted, ready, blocked, or deferred work instead of leaving completed features as generic draft.
- **SC-006**: The alignment can be reviewed without executing the application because all outcomes are documentation, specification, and sign-off consistency outcomes.

## Assumptions

- This feature is documentation/specification alignment only and does not modify runtime behavior.
- Canonical role codes are the backend role codes already used by the current user-management documentation.
- Phase 1 acceptance should include warehouse and notification capabilities because they are listed in the Phase 1 scope.
- When a verification item cannot be completed immediately, an explicit deferred record is acceptable if it includes owner, reason, and acceptance impact.
- Final status wording may be standardized during planning, but it must be more precise than leaving all completed specifications as `Draft`.

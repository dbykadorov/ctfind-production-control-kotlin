# Research: Phase 1 Alignment

## Decision: Use Current Backend Role Catalog as Canonical Vocabulary

**Decision**: The canonical Phase 1 backend role codes are `ADMIN`, `ORDER_MANAGER`, `WAREHOUSE`, `PRODUCTION_SUPERVISOR`, and `PRODUCTION_EXECUTOR`.

**Rationale**: These codes are already documented in the current admin manual and user-management contracts. Using them avoids reintroducing the older `WAREHOUSE_MANAGER` wording and keeps labels separate from authorization codes.

**Alternatives considered**:

- Keep both `WAREHOUSE` and `WAREHOUSE_MANAGER`: rejected because it preserves ambiguity in requirements and contracts.
- Rename the backend role to `WAREHOUSE_MANAGER`: rejected because it would imply product/runtime behavior changes outside this documentation alignment feature.

## Decision: Treat Human Labels as Labels, Not Role Codes

**Decision**: Human-facing labels such as "Warehouse", "Administrator", "Shop Supervisor", and Russian role names may remain in explanatory prose, but access rules must map to canonical backend role codes.

**Rationale**: Product documentation needs readable role names, while contracts and requirements need stable codes. Mixing the two caused the Phase 1 inconsistency.

**Alternatives considered**:

- Replace every label with backend code: rejected because it would make non-technical documentation harder to read.
- Keep labels without mappings: rejected because access expectations remain ambiguous.

## Decision: Phase 1 Acceptance Includes Warehouse and Internal Notifications

**Decision**: Phase 1 completion criteria should include warehouse materials, stock consumption under orders, and internal notifications unless a specific item is explicitly deferred.

**Rationale**: The Phase 1 scope document lists warehouse and internal notifications as Phase 1 components. Completion criteria should not silently omit them.

**Alternatives considered**:

- Leave completion criteria narrow: rejected because it allows incomplete sign-off.
- Move warehouse and notifications out of Phase 1 globally: rejected because that is a product-scope change, not an alignment cleanup.

## Decision: Verification Items Need a Disposition, Not Just a Checkbox

**Decision**: Each open verification item must end in one of these dispositions: passed with evidence, failed with follow-up, blocked with owner, or deferred with owner/reason/sign-off impact.

**Rationale**: Some checks are manual or environment-dependent. The important release-control requirement is that the state is explicit and reviewable.

**Alternatives considered**:

- Mark all remaining items complete without evidence: rejected because it violates Docker/verifiability expectations.
- Keep unchecked items indefinitely: rejected because it prevents a clean Phase 1 closeout.

## Decision: Use Explicit Spec Status Values

**Decision**: Phase 1 specs should use explicit status wording that distinguishes accepted, pending verification, deferred, blocked, and superseded/replaced states.

**Rationale**: `Draft` no longer communicates the real state once a feature has been implemented or reviewed. A small status vocabulary keeps future planning readable.

**Alternatives considered**:

- Use only `Draft` and task checkboxes: rejected because status remains stale at a glance.
- Invent a complex lifecycle: rejected because this is a repository-local cleanup, not a process overhaul.

## Decision: Convert 008 Task List to Checkboxes Conservatively

**Decision**: Convert actionable `specs/008-notifications-infrastructure/tasks.md` entries to checkbox tasks. Mark completion only when the implementation/evidence supports it; otherwise leave as open.

**Rationale**: The current list is not machine-trackable. The conversion should not fabricate completion.

**Alternatives considered**:

- Leave the format unchanged: rejected because Phase 1 completion tracking stays inconsistent.
- Mark every task complete during format conversion: rejected because format cleanup is not completion evidence.

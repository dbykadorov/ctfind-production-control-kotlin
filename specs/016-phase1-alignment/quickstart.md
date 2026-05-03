# Quickstart: Phase 1 Alignment Validation

This quickstart validates the documentation/specification alignment without changing runtime behavior.

## 0. Alignment Inventory

Baseline commands used before implementation:

```bash
sed -n '/## 8\\. Критерии завершения/,$p' docs/PHASE_01.md
rg -n "WAREHOUSE_MANAGER|Order Corrector|Shop Supervisor|Executor|Warehouse|Administrator" docs specs
rg -n "^- \\[ \\]|DEFERRED|BLOCKED|Pending manual sign-off" specs/007-audit-log-viewer specs/015-pam-dark-theme-sync
rg -n "\\*\\*Status\\*\\*" specs/*/spec.md
rg --pcre2 -n "^- T[0-9]{3}(?!,| \\()" specs/008-notifications-infrastructure/tasks.md
```

Baseline findings on 2026-05-03:

- `docs/PHASE_01.md` completion criteria omitted warehouse materials, stock consumption, internal notifications, board/executor workflow wording, and explicit role-management wording.
- `WAREHOUSE_MANAGER` appeared as current access vocabulary in `specs/011-warehouse-materials/spec.md`, `specs/011-warehouse-materials/checklists/requirements.md`, and `specs/012-order-bom-consumption/spec.md`.
- `specs/007-audit-log-viewer/tasks.md` had five unchecked verification tasks; `specs/015-pam-dark-theme-sync/tasks.md` had one unchecked manual sign-off task.
- Most Phase 1 specs still had generic `Status: Draft`, while `specs/005-production-tasks/spec.md` still said `Ready for implementation`.
- `specs/008-notifications-infrastructure/tasks.md` used bare `- T###` task lines instead of checkbox states.

## Status Vocabulary

- `Accepted`: implemented and reviewable for Phase 1; any remaining non-blocking verification is explicitly recorded.
- `Pending verification`: implementation exists, but one or more verification/sign-off items still need owner action before full acceptance.
- `Deferred`: work or acceptance moved outside Phase 1 with owner, reason, target, and sign-off impact.
- `Blocked`: work or verification cannot proceed until a stated blocker is resolved.
- `Superseded`: artifact is retained for history but replaced by a later spec or decision.

## Verification Disposition Record

Use this record for open verification items:

```text
Item:
Disposition: passed | failed | blocked | deferred
Owner:
Reason:
Target:
Sign-off impact:
Evidence:
Date:
```

`Owner`, `Reason`, `Target`, and `Sign-off impact` are required for deferred items. `Evidence` and `Date` are required for passed items.

## Role Vocabulary Mapping

Canonical backend role codes:

- `ADMIN` — Administrator / Администратор
- `ORDER_MANAGER` — Order Manager / Менеджер заказов
- `WAREHOUSE` — Warehouse / Склад
- `PRODUCTION_SUPERVISOR` — Production Supervisor / Мастер / начальник цеха
- `PRODUCTION_EXECUTOR` — Production Executor / Исполнитель

Human-facing labels may remain in prose, but access rules must use or map to the canonical codes above.

## 1. Confirm Active Feature

```bash
cat .specify/feature.json
```

Expected: `specs/016-phase1-alignment`.

Result 2026-05-03: PASS — `.specify/feature.json` points to `specs/016-phase1-alignment`.

## 2. Validate Role Vocabulary

Search for non-canonical or ambiguous role names:

```bash
rg -n "WAREHOUSE_MANAGER|Order Corrector|Shop Supervisor|Executor|Warehouse|Administrator" docs specs
```

Expected:

- Current access rules use `ADMIN`, `ORDER_MANAGER`, `WAREHOUSE`, `PRODUCTION_SUPERVISOR`, or `PRODUCTION_EXECUTOR`.
- Human labels either remain clearly as labels or map to canonical codes.
- `WAREHOUSE_MANAGER` does not appear as a current backend role code.

Result 2026-05-03: PASS — current access requirements now use canonical backend role codes. Remaining `Warehouse` / `Administrator` matches are labels, contract examples, or this alignment feature's validation text.

## 3. Validate Phase 1 Completion Criteria

Review:

```bash
sed -n '/## 8\\. Критерии завершения/,$p' docs/PHASE_01.md
```

Expected: criteria cover administrator bootstrap, users/roles, orders, production tasks, board/executor workflow, overdue/status visibility, audit, internal notifications, warehouse materials, and stock consumption or explicit deferrals.

Result 2026-05-03: PASS — `docs/PHASE_01.md` completion criteria include all required acceptance areas from `contracts/phase1-acceptance-baseline.contract.md`. No product capability was removed from Phase 1 scope.

## 4. Validate Open Verification Dispositions

```bash
rg -n "^- \\[ \\]|DEFERRED|BLOCKED|Pending manual sign-off" specs/007-audit-log-viewer specs/015-pam-dark-theme-sync
```

Expected: no open item lacks disposition. Each open/deferred/blocked item has owner, reason, sign-off impact, and date, or is marked complete with evidence.

Result 2026-05-03: PASS — all known 007/015 open verification items have explicit passed or deferred disposition records. Deferred manual checks are assigned to Manual QA or Product/Design with sign-off impact recorded in their source artifacts.

## 5. Validate Spec Statuses

```bash
rg -n "\\*\\*Status\\*\\*" specs/*/spec.md
```

Expected: implemented Phase 1 specs do not remain generic `Draft`; incomplete items are marked with a precise status such as pending verification, deferred, blocked, accepted, or superseded.

Result 2026-05-03: PASS — Phase 1 specs no longer use generic `Draft` for implemented work. Specs with deferred/manual verification are marked `Pending verification`.

## 6. Validate Task Checkbox Format

```bash
rg --pcre2 -n "^- T[0-9]{3}(?!,| \\()" specs/008-notifications-infrastructure/tasks.md
rg -n "^- \\[[ Xx]\\] T[0-9]" specs/008-notifications-infrastructure/tasks.md
```

Expected: actionable tasks in `008` use checkbox states. The first command should return no actionable task lines without checkbox state.

Result 2026-05-03: PASS — actionable 008 tasks use checkbox states. T028 remains open because full quickstart validation against a running Docker stack was not evidenced in the 008 artifact itself.

## 7. Review Summary

Record the final result in the implementation summary:

- Role vocabulary check result.
- Phase 1 completion criteria check result.
- Verification disposition result for 007 and 015.
- Spec status review result.
- 008 task-format review result.

Runtime commands are not required for this documentation-only feature unless implementation chooses to close runtime verification items with fresh evidence.

## 8. Final Validation Summary

- Role vocabulary check result: PASS on 2026-05-03.
- Phase 1 completion criteria check result: PASS on 2026-05-03.
- Verification disposition result for 007 and 015: PASS on 2026-05-03.
- Spec status review result: PASS on 2026-05-03.
- 008 task-format review result: PASS on 2026-05-03.
- Repository hygiene: `git status --short --branch --untracked-files=all` shows only tracked documentation/specification changes for this alignment feature; no unrelated untracked `micro` entry is currently reported.

# Quickstart: Phase 1 Alignment Validation

This quickstart validates the documentation/specification alignment without changing runtime behavior.

## 1. Confirm Active Feature

```bash
cat .specify/feature.json
```

Expected: `specs/016-phase1-alignment`.

## 2. Validate Role Vocabulary

Search for non-canonical or ambiguous role names:

```bash
rg -n "WAREHOUSE_MANAGER|Order Corrector|Shop Supervisor|Executor|Warehouse|Administrator" docs specs
```

Expected:

- Current access rules use `ADMIN`, `ORDER_MANAGER`, `WAREHOUSE`, `PRODUCTION_SUPERVISOR`, or `PRODUCTION_EXECUTOR`.
- Human labels either remain clearly as labels or map to canonical codes.
- `WAREHOUSE_MANAGER` does not appear as a current backend role code.

## 3. Validate Phase 1 Completion Criteria

Review:

```bash
sed -n '/## 8\\. Критерии завершения/,$p' docs/PHASE_01.md
```

Expected: criteria cover administrator bootstrap, users/roles, orders, production tasks, board/executor workflow, overdue/status visibility, audit, internal notifications, warehouse materials, and stock consumption or explicit deferrals.

## 4. Validate Open Verification Dispositions

```bash
rg -n "^- \\[ \\]|DEFERRED|BLOCKED|Pending manual sign-off" specs/007-audit-log-viewer specs/015-pam-dark-theme-sync
```

Expected: no open item lacks disposition. Each open/deferred/blocked item has owner, reason, sign-off impact, and date, or is marked complete with evidence.

## 5. Validate Spec Statuses

```bash
rg -n "\\*\\*Status\\*\\*" specs/*/spec.md
```

Expected: implemented Phase 1 specs do not remain generic `Draft`; incomplete items are marked with a precise status such as pending verification, deferred, blocked, accepted, or superseded.

## 6. Validate Task Checkbox Format

```bash
rg -n "^- T[0-9]" specs/008-notifications-infrastructure/tasks.md
rg -n "^- \\[[ Xx]\\] T[0-9]" specs/008-notifications-infrastructure/tasks.md
```

Expected: actionable tasks in `008` use checkbox states. The first command should return no actionable task lines without checkbox state.

## 7. Review Summary

Record the final result in the implementation summary:

- Role vocabulary check result.
- Phase 1 completion criteria check result.
- Verification disposition result for 007 and 015.
- Spec status review result.
- 008 task-format review result.

Runtime commands are not required for this documentation-only feature unless implementation chooses to close runtime verification items with fresh evidence.

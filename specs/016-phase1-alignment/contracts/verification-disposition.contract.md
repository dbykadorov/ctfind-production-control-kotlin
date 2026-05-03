# Contract: Verification Disposition

## Allowed Dispositions

| Disposition | Meaning | Required Fields |
|-------------|---------|-----------------|
| `passed` | Check completed successfully | Evidence, date |
| `failed` | Check completed and exposed a problem | Evidence, owner, sign-off impact, date |
| `blocked` | Check cannot currently run | Owner, reason, sign-off impact, date |
| `deferred` | Check intentionally moved outside current sign-off path | Owner, reason, target, sign-off impact, date |

## Required Open-Item Handling

The following known open items must receive a disposition during implementation:

- `specs/007-audit-log-viewer/tasks.md` open backend Docker test item.
- `specs/007-audit-log-viewer/tasks.md` open Docker startup and health item.
- `specs/007-audit-log-viewer/tasks.md` open admin manual smoke item.
- `specs/007-audit-log-viewer/tasks.md` open executor/supervisor manual smoke item.
- `specs/007-audit-log-viewer/tasks.md` open tablet smoke item.
- `specs/015-pam-dark-theme-sync/tasks.md` open dark-theme sign-off item.

## Rules

- No item should remain as an unchecked task with no disposition note.
- Passed items should reference the command output, quickstart record, sign-off table, or review note that proves completion.
- Deferred items should state whether Phase 1 can be signed off before completion.
- A blocked item is not the same as deferred; blocked means it still matters for the same sign-off path unless explicitly moved.

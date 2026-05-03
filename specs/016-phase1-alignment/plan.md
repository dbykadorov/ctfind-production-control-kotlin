# Implementation Plan: Phase 1 Alignment

**Branch**: `016-phase1-alignment` | **Date**: 2026-05-03 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/016-phase1-alignment/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Align Phase 1 documentation and specification artifacts so the phase can be reviewed against a single acceptance baseline. The work is documentation/specification only: normalize canonical role vocabulary, resolve or explicitly defer open verification items, update Phase 1 completion criteria, finalize spec statuses, and make the notifications infrastructure task list trackable with checkbox states.

## Technical Context

**Language/Version**: Markdown documentation and Spec Kit artifacts  
**Primary Dependencies**: Existing `docs/`, `specs/`, `.specify/`, and root AGENTS guidance  
**Storage**: Repository files only  
**Testing**: Text/traceability review, repository searches, task/status checklist validation, optional root Makefile verification if verification items are closed rather than deferred  
**Target Platform**: Repository documentation consumed by maintainers, reviewers, and planning agents  
**Project Type**: Documentation/specification alignment in a Kotlin/Spring Boot + Vue project  
**Performance Goals**: A reviewer can identify Phase 1 acceptance scope in under 10 minutes; role-vocabulary checks complete with simple repository search  
**Constraints**: No runtime behavior changes; no permission semantics changes; preserve Docker-first verification evidence requirements; keep edits scoped to docs/spec artifacts unless validation requires command output references  
**Scale/Scope**: Phase 1 artifacts across `docs/PHASE_01.md`, `docs/PHASE_01_MANUAL.md`, `specs/001` through `specs/015`, and this feature's planning artifacts

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **ERP domain fit**: PASS. The feature strengthens the Phase 1 operational baseline across orders, production tasks, inventory, users, roles, audit, and internal notifications.
- **Constraint-aware operations**: PASS. The feature preserves existing TOC-relevant facts by making task status, overdue visibility, inventory usage, audit, and verification scope explicit rather than adding new operational state.
- **Architecture boundaries**: PASS. No application, controller, persistence, or frontend code changes are planned. Documentation changes must not move or redefine business rules.
- **Traceability/audit**: PASS. The plan requires explicit records for status changes, verification dispositions, and deferred work.
- **API-only/security**: PASS. Backend remains API-only. Role work is vocabulary alignment only and must not change authorization behavior.
- **Docker/verifiability**: PASS. Runtime is unchanged. If open verification items are closed, evidence must be recorded; if not, the deferral must include owner, reason, and sign-off impact.
- **Exception handling**: No constitution exceptions identified.

## Project Structure

### Documentation (this feature)

```text
specs/016-phase1-alignment/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── phase1-acceptance-baseline.contract.md
│   ├── role-vocabulary.contract.md
│   └── verification-disposition.contract.md
└── tasks.md
```

### Source Code (repository root)

```text
docs/
├── PHASE_01.md
├── PHASE_01_MANUAL.md
└── tech-debt.md

specs/
├── 001-local-docker-startup/
├── ...
├── 015-pam-dark-theme-sync/
└── 016-phase1-alignment/

AGENTS.md
.specify/
```

**Structure Decision**: This is a repository documentation/specification alignment feature. The implementation will edit existing Phase 1 documentation and specification artifacts plus the new `specs/016-phase1-alignment/` planning files. No backend or frontend source directories are part of the planned write scope.

## Complexity Tracking

No constitution violations or extra complexity exceptions.

## Phase 0: Research

Research is complete in [research.md](./research.md). Key decisions:

- Canonical backend role vocabulary is fixed to the current role catalog: `ADMIN`, `ORDER_MANAGER`, `WAREHOUSE`, `PRODUCTION_SUPERVISOR`, `PRODUCTION_EXECUTOR`.
- Phase 1 acceptance criteria must include warehouse and internal notifications because they are part of the documented Phase 1 scope.
- Open verification items must be resolved with either fresh evidence or explicit deferral metadata.
- Spec status values should distinguish accepted, pending verification, deferred, and replaced/superseded states.
- The notifications infrastructure task list must be converted to checkbox format without implying completion unless evidence supports it.

## Phase 1: Design & Contracts

Design artifacts generated:

- [data-model.md](./data-model.md) defines the documentation entities and validation rules for acceptance baseline, role vocabulary, verification items, spec status, and deferred follow-up.
- [contracts/phase1-acceptance-baseline.contract.md](./contracts/phase1-acceptance-baseline.contract.md) defines the required acceptance baseline.
- [contracts/role-vocabulary.contract.md](./contracts/role-vocabulary.contract.md) defines allowed canonical role codes and label mapping rules.
- [contracts/verification-disposition.contract.md](./contracts/verification-disposition.contract.md) defines valid verification and deferral records.
- [quickstart.md](./quickstart.md) defines the manual validation flow for this documentation/spec alignment.

## Post-Design Constitution Check

- **ERP domain fit**: PASS. Contracts cover all Phase 1 ERP/control areas named in the spec.
- **Constraint-aware operations**: PASS. No TOC facts are removed; acceptance wording keeps overdue/status/audit/inventory usage visible.
- **Architecture boundaries**: PASS. Design is documentation-only and does not introduce source-code architecture changes.
- **Traceability/audit**: PASS. Verification and status contracts require explicit evidence or deferral records.
- **API-only/security**: PASS. Role-vocabulary contract preserves current role semantics and forbids behavior changes in this feature.
- **Docker/verifiability**: PASS. Quickstart includes repository searches and documentation checks; runtime commands are required only if the implementation chooses to close runtime verification items with fresh evidence.
- **Exception handling**: No exceptions.

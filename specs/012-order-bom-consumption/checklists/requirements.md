# Specification Quality Checklist: Списание материалов под заказ + BOM

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-04-29
**Feature**: [spec.md](../spec.md)

## Content Quality

- [X] No implementation details (languages, frameworks, APIs)
- [X] Focused on user value and business needs
- [X] Written for non-technical stakeholders
- [X] All mandatory sections completed

## Requirement Completeness

- [X] No [NEEDS CLARIFICATION] markers remain
- [X] Requirements are testable and unambiguous
- [X] Success criteria are measurable
- [X] Success criteria are technology-agnostic (no implementation details)
- [X] All acceptance scenarios are defined
- [X] Edge cases are identified
- [X] Scope is clearly bounded
- [X] Dependencies and assumptions identified

## Feature Readiness

- [X] All functional requirements have clear acceptance criteria
- [X] User scenarios cover primary flows
- [X] Feature meets measurable outcomes defined in Success Criteria
- [X] No implementation details leak into specification

## Notes

- API path examples (`/api/orders/{orderId}/bom`, `/api/materials/{id}/consume`) and entity names (`OrderMaterialRequirement`, `OrderLookupPort`) appear in **Assumptions** section as decisions inherited from the brief; these are intentionally kept here so `/speckit-plan` has a starting point. They could be tightened further if the spec must be fully implementation-agnostic — flag for review.
- RBAC default: BOM редактирует только Order Manager + Administrator. Если бизнес-сторона захочет дать редактирование Production Supervisor — это решается одной строкой в `/speckit-clarify` или прямо в plan.md без переписывания спеки.
- Перерасход по BOM: warning, не блокировка. Если на стороне бизнеса политика жёсткая — изменить FR-011 на блокировку.
- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`

# Implementation Plan: Синхронизация тёмной темы с PAM UC

**Branch**: `015-pam-dark-theme-sync` | **Date**: 2026-04-30 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `specs/015-pam-dark-theme-sync/spec.md`

## Summary

Выровнять тёмную тему Vue 3 кабинета CTfind с семантической моделью эталона PAM User Console: обновить CSS design tokens (`tokens.css`), при необходимости мост Tailwind (`tailwind.config.js`), добиться согласованных поверхностей/текста/бордеров/оверлея и состояний disabled/hover на ключевых паттернах UI, сохранив **amber** акцент и **белую `.cabinet-card`** как elevated island. Зафиксировать матрицу соответствия и отступления в `contracts/`. Добавить при необходимости паттерн кратковременного подавления CSS transitions при смене темы (аналог `.theme-switching` в PAM). Backend и API не затрагиваются.

## Technical Context

**Language/Version**: TypeScript 5.x (strict), Vue 3 (Composition API)  
**Primary Dependencies**: Vue 3, Pinia, vue-router, vue-i18n, Tailwind CSS, Vite  
**Storage**: `localStorage` ключ `ctfind.cabinet.theme.v2` (theme + sidebarPreset) — без изменений модели данных на сервере  
**Testing**: `pnpm vitest run`, `pnpm typecheck`, `pnpm lint`; при необходимости узкие snapshot/строковые тесты для токенов (опционально)  
**Target Platform**: Browser SPA (desktop primary, responsive)  
**Project Type**: Web application frontend (cabinet only)  
**Performance Goals**: Переключение темы воспринимается мгновенно; без дополнительных сетевых запросов  
**Constraints**: Не портировать PrimeVue; не синхронизировать светлую тему с PAM в этом инкременте; WCAG 2.1 AA для зафиксированных пар в матрице  
**Scale/Scope**: Преимущественно `frontend/cabinet/src/styles/tokens.css`, `globals.css`, layout (`AppShell`, `Sidebar`, `TopBar`), общие UI-примитивы; репрезентативные страницы для QA по `quickstart.md`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **ERP domain fit**: Укрепляет единообразие операционного UI Phase 01; доменные сущности заказов/производства не меняются.

- **Constraint-aware operations**: Факты для TOC / lead time не затрагиваются; улучшается только восприятие интерфейса.

- **Architecture boundaries**: Логика темы остаётся в презентационном слое (CSS tokens, Pinia UI store, разметка layout). Бизнес-правила и use cases backend не меняются.

- **Traceability/audit**: Новые бизнес-события аудита не требуются.

- **API-only/security**: Backend остаётся API-only; авторизация и роли не затрагиваются.

- **Docker/verifiability**: `make frontend-test`, `make frontend-build` (или эквивалент `pnpm test && pnpm build` из `frontend/cabinet`); `make docker-up-detached && make health` без регрессий; ручная проверка по `quickstart.md`.

- **Exception handling**: Нарушений конституции нет.

### Post-design re-check

Артефакты Phase 1 (`research.md`, `data-model.md`, `contracts/*`) описывают только клиентские токены и контракты верификации; границы домена и API сохранены.

## Project Structure

### Documentation (this feature)

```text
specs/015-pam-dark-theme-sync/
├── plan.md              # This file
├── spec.md
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── theme-pam-alignment.contract.md
│   └── qa-signoff-routes.contract.md
└── tasks.md             # Phase 2 (/speckit-tasks — не создаётся этой командой)
```

### Source Code (repository root)

```text
frontend/cabinet/
├── src/
│   ├── styles/
│   │   ├── tokens.css           # PRIMARY: dark theme token values + aliases
│   │   └── globals.css          # layout/shell overrides if needed
│   ├── components/
│   │   ├── layout/
│   │   │   ├── AppShell.vue
│   │   │   ├── Sidebar.vue
│   │   │   └── TopBar.vue
│   │   └── ui/                  # primitives using semantic colors (spot-fix)
│   ├── stores/
│   │   └── ui.ts                # optional: theme-switching class toggle
│   └── pages/                   # spot fixes only if hardcoded colors escape tokens
├── tailwind.config.js           # sync extended colors with tokens if needed
├── index.html                   # inline FOUC script: verify data-theme attrs unchanged
└── tests/                       # optional token/regression tests
```

**Structure Decision**: Одно фронтенд-приложение `frontend/cabinet`; изменения сосредоточены в слое дизайн-токенов и layout/UI, без нового пакета и без backend-модуля.

## Complexity Tracking

> Не заполняется — отклонений от принципов конституции нет.

## Phase 0 & Phase 1 Outputs (this run)

| Artifact | Purpose |
|----------|---------|
| `research.md` | Решения по маппингу PAM→CTfind, transitions, матрица |
| `data-model.md` | Клиентские сущности: тема, пресет sidebar, версия матрицы |
| `contracts/theme-pam-alignment.contract.md` | Роли цветов, допуски, отступления |
| `contracts/qa-signoff-routes.contract.md` | Минимальный набор маршрутов QA (FR-010) |
| `quickstart.md` | Ручная верификация и регресс пресетов |

## Next Steps

После утверждения плана: `/speckit-tasks` для генерации `tasks.md`, затем имплементация по задачам.

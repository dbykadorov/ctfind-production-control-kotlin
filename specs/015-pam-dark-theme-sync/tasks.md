---
description: "Task list for 015 PAM dark theme alignment"
---

# Tasks: Синхронизация тёмной темы с PAM UC

**Input**: Design documents from `/specs/015-pam-dark-theme-sync/`  
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/)

**Tests**: В спецификации явный TDD не запрошен — отдельные задачи на автотесты не включены; верификация через `quickstart.md`, контракт QA и `pnpm`/Docker проверки.

**Organization**: Задачи сгруппированы по user story из `spec.md` для независимой приёмки.

**Constitution**: Backend и домен не меняются; сохраняются Docker-first проверки и границы SPA.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: можно параллелить (разные файлы, нет зависимости от незавершённой задачи)
- **[Story]**: метка user story ([US1], [US2], [US3])
- В описании указаны конкретные пути файлов

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Зафиксировать расхождения эталона и кабинета до правок токенов.

- [X] T001 Audit dark palette: сопоставить PAM `_vars.scss` (`:root[data-theme="dark"]`) с текущими значениями `[data-theme='dark']` в `frontend/cabinet/src/styles/tokens.css` и занести черновые строки/заметки в `specs/015-pam-dark-theme-sync/contracts/theme-pam-alignment.contract.md`
- [X] T002 [P] Audit Tailwind semantic colors: проверить соответствие `frontend/cabinet/tailwind.config.js` токенам dark/light и отметить расхождения в `specs/015-pam-dark-theme-sync/contracts/theme-pam-alignment.contract.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Утверждённая матрица соответствия до изменения production CSS.

**⚠️ CRITICAL**: User Story задачи не начинать до завершения T003–T004.

- [X] T003 Заполнить колонки Status и Notes для всех строк матрицы в `specs/015-pam-dark-theme-sync/contracts/theme-pam-alignment.contract.md` (включая отступления/решения по `.cabinet-card` и amber акценту)
- [X] T004 Обновить поле `matrixVersion` (и при наличии `pamReferenceCommitOrTag`) в шапке `specs/015-pam-dark-theme-sync/contracts/theme-pam-alignment.contract.md` после согласования матрицы

**Checkpoint**: Матрица готова — можно менять токены и UI.

---

## Phase 3: User Story 1 — Единообразная тёмная тема с эталоном (Priority: P1) 🎯 MVP

**Goal**: Палитра dark (viewport, текст, бордеры, оверлей, статусы, состояния контролов) согласована с матрицей и эталоном PAM по смыслу.

**Independent Test**: Ручная проверка маршрутов из `specs/015-pam-dark-theme-sync/contracts/qa-signoff-routes.contract.md` в dark; контраст ключевых пар по матрице.

### Implementation for User Story 1

- [X] T005 [US1] Применить согласованные значения dark-токенов (`--bg-app`, `--bg-app-grad-top`, `--bg-app-grad-bottom`, `--c-fg*`, `--c-border*`, `--c-overlay`, `--c-status-*`, `--tooltip-*` при необходимости) в `frontend/cabinet/src/styles/tokens.css` строго по строкам матрицы `specs/015-pam-dark-theme-sync/contracts/theme-pam-alignment.contract.md`
- [X] T006 [P] [US1] Выровнять затемнение модалок/диалогов с политикой `--c-overlay`: `frontend/cabinet/src/components/ui/dialog/Dialog.vue`, телепортированные оверлеи в `frontend/cabinet/src/pages/admin/UsersPage.vue`, и другие найденные по поиску `bg-black/` или хардкода оверлея в `frontend/cabinet/src/`
- [X] T007 [P] [US1] Привести disabled/hover/focus видимость форм и кнопок к правилам матрицы: `frontend/cabinet/src/styles/globals.css` и при необходимости примитивы в `frontend/cabinet/src/components/ui/`

**Checkpoint**: US1 визуально и по контрасту соответствует матрице для ключевых экранов.

---

## Phase 4: User Story 2 — Сохранение узнаваемости CTfind (Priority: P1)

**Goal**: Amber-бренд и модель elevated `.cabinet-card` на тёмном хроме сохранены; отступления только в матрице.

**Independent Test**: Чеклист бренда — акцентные кнопки/ссылки остаются в шкале brand; карточка контента читаема (WCAG по матрице).

### Implementation for User Story 2

- [X] T008 [US2] Подтвердить отсутствие не согласованных изменений `--c-brand-*` в `frontend/cabinet/src/styles/tokens.css`; любые дельты оформить в матрице как `delta`/`waived` с обоснованием
- [X] T009 [US2] Проверить и при необходимости скорректировать блок `[data-theme='dark'] .cabinet-card` в `frontend/cabinet/src/styles/tokens.css` для WCAG AA по парам из матрицы
- [X] T010 [P] [US2] Устранить хардкод цветов в chrome layout: `frontend/cabinet/src/components/layout/Sidebar.vue`, `frontend/cabinet/src/components/layout/TopBar.vue`, `frontend/cabinet/src/components/layout/AppShell.vue` — заменить на токены/Tailwind семантику из `tokens.css`

**Checkpoint**: US1+US2 вместе дают эталонное выравнивание без потери идентичности CTfind.

---

## Phase 5: User Story 3 — Переключение темы без артефактов (Priority: P2)

**Goal**: Нет устойчивого мигания при light↔dark из-за CSS transitions (FR-008).

**Independent Test**: Пять циклов переключения темы без артефактов по `specs/015-pam-dark-theme-sync/quickstart.md`.

### Implementation for User Story 3

- [X] T011 [US3] Реализовать кратковременный класс подавления transitions при смене темы в `frontend/cabinet/src/stores/ui.ts` (логика `setTheme`) по `specs/015-pam-dark-theme-sync/research.md` §R-004
- [X] T012 [US3] Добавить правила для класса переключения темы (например `.theme-switching`) в `frontend/cabinet/src/styles/globals.css`, ограничив затронутые свойства как в эталоне PAM `_theme-dark.scss` / `.theme-switching`

**Checkpoint**: Переключение темы стабильно визуально.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Качество, Docker, документация.

- [X] T013 Запустить `pnpm typecheck`, `pnpm lint`, `pnpm test`, `pnpm build` в `frontend/cabinet`; затем в корне репозитория `make docker-up-detached && make health` — зафиксировать результат в описании PR или заметке к фиче
- [X] T014 Пройти сценарии `specs/015-pam-dark-theme-sync/quickstart.md` и заполнить таблицу Sign-off в `specs/015-pam-dark-theme-sync/contracts/qa-signoff-routes.contract.md` — disposition recorded 2026-05-03: Product/Design manual visual sign-off deferred with owner and sign-off impact in the contract.
- [X] T015 [P] Синхронизировать формулировки при расхождении реализации с матрицей: обновить при необходимости `specs/015-pam-dark-theme-sync/plan.md` и/или `specs/015-pam-dark-theme-sync/contracts/theme-pam-alignment.contract.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1** → **Phase 2** → **Phase 3 (US1)** → **Phase 4 (US2)** → **Phase 5 (US3)** → **Phase 6**
- **US3** технически слабо зависит от точных hex US1/US2; допускается начинать T011–T012 после **Phase 2**, если не конфликтует с командой (рекомендуемый порядок: после US1/US2 для одной ветки приёмки).

### User Story Dependencies

- **US1**: После Phase 2; не зависит от US2/US3.
- **US2**: После US1 (нужны финальные токены и поведение карточки).
- **US3**: После Phase 2 минимум для кода; полная приёмка после US1/US2 предпочтительна.

### Parallel Opportunities

- **Phase 1**: T001 и T002 параллельно.
- **US1**: T006 и T007 параллельно после T005 (или T005 частично скоординировать с ними при одной матрице).
- **US2**: T010 параллельно с T008–T009 после завершения T005.
- **Phase 6**: T015 параллельно с подготовкой отчёта T014 (разные файлы).

### Parallel Example: User Story 1

```bash
# После T005 или совместно с согласованием матрицы:
Task T006 — Dialog.vue + UsersPage.vue + grep оверлеев
Task T007 — globals.css + components/ui/*
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1–2  
2. Phase 3 (US1) — остановка и визуальная приёмка по QA-маршрутам  
3. При необходимости демо перед US2

### Incremental Delivery

1. US1 — палитра и оверлеи  
2. US2 — бренд и карточка + layout chrome  
3. US3 — переключение без артефактов  
4. Polish — CI/Docker/sign-off

---

## Notes

- Любое изменение hex после sign-off матрицы → bump `matrixVersion` в `contracts/theme-pam-alignment.contract.md`.
- Не тянуть SCSS из репозитория PAM в сборку CTfind; только смысловое соответствие.

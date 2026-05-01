# Contract: Theme alignment with PAM UC (dark)

**Feature**: `015-pam-dark-theme-sync`  
**Spec**: [../spec.md](../spec.md)

## Purpose

Зафиксировать семантическое соответствие между эталоном PAM User Console (dark) и кабинетом CTfind без обязательства пиксельной идентичности всего продукта PAM.

## Reference locations

| Source | Absolute path |
|--------|----------------|
| PAM palette | `/home/dbykadorov/Work/projects/pam/services/frontend/uc/src/assets/scss/_vars.scss` |
| PAM dark overrides | `/home/dbykadorov/Work/projects/pam/services/frontend/uc/src/assets/scss/_theme-dark.scss` |
| Cabinet tokens | `/home/dbykadorov/Work/projects/ctfind-production-control-contlin/frontend/cabinet/src/styles/tokens.css` |
| Tailwind bridge | `/home/dbykadorov/Work/projects/ctfind-production-control-contlin/frontend/cabinet/tailwind.config.js` |

## Non-negotiables (CTfind product)

1. **Brand accent**: доминирующий акцент остаётся amber (`--c-brand-*`), если продукт явно не согласует отступление.
2. **Floating card**: основная контентная область остаётся «островной» картой поверх chrome, но её surface theme-aware (dark в тёмной теме, light в светлой).

## Matrix template

При имплементации заполнить таблицу; статус каждой строки — `match` | `delta` | `waived`.

| roleId | PAM reference (semantic) | Cabinet token / surface | Status | Notes / tolerance |
|--------|-------------------------|-------------------------|--------|-------------------|
| app.viewport.top | `--main-bg` / grad stops | `--bg-app-grad-top` | match | Выравнено к тёмной PAM-ступени (`theme100`), допускается мягкий градиент для глубины. |
| app.viewport.bottom | gradient end | `--bg-app-grad-bottom` | match | Нижняя ступень приведена к `theme200` роли PAM dark. |
| app.chrome.solid | `--theme100` (dark bg base) | `--bg-app` | match | Базовый chrome использует тёмную нейтральную опору PAM dark. |
| sidebar.fg.default | menu idle | `--c-fg` / sidebar rules | match | Sidebar-foreground переведён на семантические токены (`--sidebar-fg-default`). |
| sidebar.fg.active | active/hover panels | scoped sidebar CSS | match | Hover/active фон и текст вынесены в токены (`--sidebar-item-*`), визуально эквивалентны PAM-паттерну. |
| text.strong | `--theme800` (dark = light text) | `--c-fg-strong` (outside card) | match | Сильный текст на dark chrome остаётся высококонтрастным. |
| text.default | mid greys | `--c-fg` | match | Основной текст соответствует средней контрастной роли PAM. |
| text.muted | `--theme500`–`600` | `--c-fg-muted` | match | Приглушённый текст приведён к роли `theme500/600` эквивалента. |
| border.default | `--theme300` | `--c-border` / `--c-border-strong` | match | Контуры dark-контролов и chrome-разделителей соответствуют роли `theme300`. |
| overlay.modal | `--overlay` | `--c-overlay` / modal backdrop | match | `--c-overlay` нормализован к ~black/60% и применён в модалках (`bg-overlay`). |
| surface.card | raised panels PAM | `--c-surface` inside `.cabinet-card` | match | Surface карточки переведён на dark neutral, elevated-эффект сохранён радиусом/тенью. |
| status.* | badge vars | `--c-status-*` | delta | Сохраняем семантику статусов CTfind; оттенки не 1:1 PAM, но различимость подтверждается QA. |
| disabled.control | `_theme-dark.scss` disabled | input/button disabled styles | match | Добавлены единые правила disabled для form-controls в dark (`globals.css`). |

Дополнительные строки добавлять по мере аудита компонентов (таблицы, формы, dropdown).

## WCAG checklist hook

Для каждой строки, влияющей на читаемость текста или контраст контролов, добавить подстроку в приложении к матрице: «пара контраста» + инструмент проверки + скрин/значение ratio.

## Tailwind bridge audit

- `frontend/cabinet/tailwind.config.js` использует семантический мост: `bg`, `surface`, `overlay`, `ink.*`, `border.*`, `status.*`.
- Обнаруженный разрыв: часть layout/sidebar цветов была хардкодом (`text-white/*`, `rgba(255,255,255,...)`) вне токенов; устранено переносом в CSS variables.
- Прямая шкала `--theme100..800` не добавлялась (решение R-001); применён семантический слой токенов CTfind.

## Change control

Любое изменение hex/rgb токенов в `[data-theme='dark']` после sign-off должно обновлять `matrixVersion` в шапке этого файла (ISO date достаточно).

**pamReferenceCommitOrTag**: `N/A (path-based snapshot audit)`
**matrixVersion**: `2026-05-02` (implemented)

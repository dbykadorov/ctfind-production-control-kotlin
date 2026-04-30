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
2. **Floating card**: основная контентная область остаётся светлой «островной» картой на тёмном хроме (`[data-theme='dark'] .cabinet-card` перебивка сохраняется), если не согласовано иное.

## Matrix template

При имплементации заполнить таблицу; статус каждой строки — `match` | `delta` | `waived`.

| roleId | PAM reference (semantic) | Cabinet token / surface | Status | Notes / tolerance |
|--------|-------------------------|-------------------------|--------|-------------------|
| app.viewport.top | `--main-bg` / grad stops | `--bg-app-grad-top` | | navy CTfind vs PAM grey scale — документировать визуальную близость |
| app.viewport.bottom | gradient end | `--bg-app-grad-bottom` | | |
| app.chrome.solid | `--theme100` (dark bg base) | `--bg-app` | | |
| sidebar.fg.default | menu idle | `--c-fg` / sidebar rules | | |
| sidebar.fg.active | active/hover panels | scoped sidebar CSS | | |
| text.strong | `--theme800` (dark = light text) | `--c-fg-strong` (outside card) | | |
| text.default | mid greys | `--c-fg` | | |
| text.muted | `--theme500`–`600` | `--c-fg-muted` | | |
| border.default | `--theme300` | `--c-border` / `--c-border-strong` | | |
| overlay.modal | `--overlay` | `--c-overlay` / modal backdrop | | target ~black 60% equiv. |
| surface.card | raised panels PAM | `--c-surface` inside `.cabinet-card` | waived | белая карточка — осознанное отступление |
| status.* | badge vars | `--c-status-*` | | сохранить семантику статусов |
| disabled.control | `_theme-dark.scss` disabled | input/button disabled styles | | |

Дополнительные строки добавлять по мере аудита компонентов (таблицы, формы, dropdown).

## WCAG checklist hook

Для каждой строки, влияющей на читаемость текста или контраст контролов, добавить подстроку в приложении к матрице: «пара контраста» + инструмент проверки + скрин/значение ratio.

## Change control

Любое изменение hex/rgb токенов в `[data-theme='dark']` после sign-off должно обновлять `matrixVersion` в шапке этого файла (ISO date достаточно).

**matrixVersion**: `2026-04-30` (draft)

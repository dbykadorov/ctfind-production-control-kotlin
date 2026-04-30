# Contract: QA sign-off routes (dark theme)

**Feature**: `015-pam-dark-theme-sync`  
**Spec**: [../spec.md](../spec.md) — FR-010

## Purpose

Минимальный набор маршрутов и состояний UI для обязательной ручной проверки после изменений тёмной темы. Список может быть расширен владельцем продукта, но не сокращаться без явного решения.

## Preconditions

- Тема: **dark** (`data-theme='dark'`).
- Аутентифицированный пользователь с типичными ролями для доступа к маршрутам (admin там, где нужно).

## Routes (baseline)

| # | Route | What to verify |
|---|-------|----------------|
| 1 | `/cabinet/login` | Фон, поля, кнопка; нет «сломанных» контрастов при первом paint (FOUC). |
| 2 | `/cabinet` | Dashboard KPI/cards внутри `.cabinet-card`, sidebar, topbar. |
| 3 | `/cabinet/orders` | Таблица/список, hover строк, пустое состояние если есть. |
| 4 | `/cabinet/users` | Таблица + модальное создание/редактирование (оверлей, формы, disabled). |
| 5 | Один складской или производственный экран по выбору команды (например `/cabinet/warehouse` или `/cabinet/production-tasks`) | Типовые формы/бейджи статусов. |
| 6 | `/cabinet/notifications` | Списки, badge в TopBar при наличии. |

## Sidebar presets regression

Для каждого значения `sidebarPreset`: `none`, `ocean`, `sunset`, `forest`, `twilight`, `graphite`:

- Переключить пресет в UI-параметрах.
- Проверить контраст текста и иконок в sidebar и tooltip (если видим) в **dark** теме.

## Theme toggle stress

Пять циклов **light ↔ dark** подряд:

- Нет устойчивого мигания или «промежуточных» цветов из-за CSS transitions (FR-008).

## Sign-off

| Role | Name | Date | Notes |
|------|------|------|-------|
| Product / Design | | | |
| QA | | | |

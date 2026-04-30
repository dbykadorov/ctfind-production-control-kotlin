# Data Model: Dark theme alignment (015)

Фича не добавляет сущностей PostgreSQL и не меняет REST API. Ниже — клиентские и документные сущности.

## 1. Theme preference (client)

Персистится в браузере (см. `frontend/cabinet/src/stores/ui.ts`).

| Field | Type | Description |
|-------|------|-------------|
| `theme` | `'dark' \| 'light'` | Активная цветовая схема; определяет `data-theme` на документе. |
| `sidebarPreset` | `'none' \| 'ocean' \| 'sunset' \| 'forest' \| 'twilight' \| 'graphite'` | Градиент/фон боковой панели; ортогонален к теме, но в dark должен оставаться контрастным и без регрессий. |

**Validation**: Невалидные значения в JSON → fallback `theme: 'dark'`, `sidebarPreset: 'none'` (существующая логика `parseTheme`).

## 2. Reference snapshot (document)

| Field | Type | Description |
|-------|------|-------------|
| `pamReferenceCommitOrTag` | string (optional) | Фиксация версии эталона PAM UC при составлении матрицы (если доступен git в смежном репо). |
| `matrixVersion` | semver или date | Версия файла `contracts/theme-pam-alignment.contract.md` для трейсинга sign-off. |

## 3. Matrix row (logical)

Каждая строка матрицы соответствия (в контракте):

| Attribute | Description |
|-----------|-------------|
| `roleId` | Семантический идентификатор роли цвета (например `surface.chrome`, `text.muted`, `border.default`). |
| `pamReference` | Указатель на переменную или правило эталона (имя `--theme*` / секция `_theme-dark.scss`). |
| `cabinetToken` | Целевой CSS variable или класс Tailwind-семантики CTfind. |
| `status` | `match` \| `waived` \| `delta` |
| `notes` | Допуски в LAB/opacity или обоснование отступления. |

## 4. Relationships

- **Theme preference** определяет, какой набор значений из `tokens.css` активен для пользователя.
- **Matrix** не хранится в localStorage; живёт в репозитории как контракт и обновляется вместе с токенами.

## 5. State transitions

- Пользователь меняет тему через UI → обновление Pinia + `data-theme` + опционально класс `theme-switching` (см. `research.md` R-004).
- Никаких серверных синхронизаций темы в Phase 01 для этой фичи не требуется.

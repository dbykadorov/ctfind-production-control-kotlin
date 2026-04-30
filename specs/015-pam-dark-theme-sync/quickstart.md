# Quickstart: Verify dark theme alignment (015)

## Prerequisites

- `pnpm install` в `frontend/cabinet`
- Dev-сервер: `pnpm dev` (или через Makefile, если обёрнут)

## Steps

1. Открыть кабинет, установить тему **Тёмная** в переключателе темы (TopBar / настройки UI).
2. Пройти маршруты из `contracts/qa-signoff-routes.contract.md` и отметить визуальные расхождения с матрицей `contracts/theme-pam-alignment.contract.md`.
3. Для каждого **sidebar preset** убедиться, что навигация читаема и нет полной потери контраста с текстом sidebar.
4. Выполнить **stress toggle** light/dark × 5; убедиться в отсутствии заметных артефактов transitions.
5. (Опционально) Проверить `prefers-reduced-motion`: системная настройка ОС не должна делать переключение темы менее доступным.

## Automated checks (development)

```bash
cd frontend/cabinet
pnpm typecheck
pnpm lint
pnpm test
pnpm build
```

Корень репозитория:

```bash
make frontend-test
make frontend-build
make docker-up-detached && make health
```

## Evidence for PR

- Обновлённая матрица с заполненными статусами строк (или ссылка на коммит, где она заполнена).
- Короткий список скриншотов ключевых экранов (до/после) при значимых визуальных изменениях.

# Quickstart: Склад — материалы и остатки

**Branch**: `011-warehouse-materials`

## Prerequisites

```bash
make docker-up-detached
make health
# → {"status":"UP"}
```

Login: `http://localhost:5173/cabinet/login` → admin / admin

## Scenario 1: Справочник материалов (US1)

### 1.1 Проверка доступа

1. Войти как admin (Administrator) → пункт «Склад» виден в Sidebar
2. Перейти на `/cabinet/warehouse` → таблица материалов (пустая)
3. Выйти, войти как пользователь без роли WAREHOUSE → пункт «Склад» НЕ виден
4. Попытка прямого перехода на `/cabinet/warehouse` → страница «Доступ запрещён»

### 1.2 CRUD материалов

1. Войти как admin или пользователь с ролью WAREHOUSE
2. Нажать «Добавить материал»
3. В модальном окне ввести: название = «Фанера берёзовая», ед.изм. = м²
4. Нажать «Создать» → материал появляется в таблице с остатком 0
5. Создать ещё: «Краска белая» (л), «Шурупы 4×40» (шт), «Брус 50×50» (м)
6. В поиске ввести «фан» → видна только «Фанера берёзовая»
7. Очистить поиск → все 4 материала видны
8. Нажать редактирование «Фанера берёзовая» → изменить название на «Фанера берёзовая 6мм» → сохранить
9. Нажать удаление «Краска белая» (без движений) → подтвердить → материал исчезает

### 1.3 Валидация

1. Попытка создать материал без названия → ошибка валидации
2. Попытка создать материал с названием «Фанера берёзовая 6мм» (дубликат) → ошибка «Материал с таким названием уже существует»

## Scenario 2: Приход материала (US2)

### 2.1 Оформление прихода

1. На странице списка материалов нажать кнопку «Приход» рядом с «Фанера берёзовая 6мм»
2. В модальном окне ввести: количество = 100, комментарий = «Поставка от ООО Лес»
3. Нажать «Оформить» → модалка закрывается
4. В таблице остаток «Фанера берёзовая 6мм» = 100
5. Повторить приход: количество = 50.5, комментарий = «Довоз»
6. Остаток = 150.5

### 2.2 Журнал движений

1. Кликнуть на «Фанера берёзовая 6мм» → открывается карточка материала (`/cabinet/warehouse/:id`)
2. Видна информация: название, ед.изм., текущий остаток = 150.5
3. Ниже — журнал движений:
   - Запись 1: Приход, 50.5, «Довоз», admin, [дата]
   - Запись 2: Приход, 100, «Поставка от ООО Лес», admin, [дата]
4. Порядок — обратный хронологический (новые сверху)

### 2.3 Валидация прихода

1. Попытка ввести количество = 0 → кнопка неактивна
2. Попытка ввести отрицательное количество → ошибка валидации

### 2.4 Блокировка удаления

1. Вернуться в список, попытка удалить «Фанера берёзовая 6мм» (есть движения) → ошибка «Невозможно удалить материал с историей движений»

## Scenario 3: Аудит (US4)

1. Войти как admin
2. Перейти в «Журнал действий» (`/cabinet/audit`)
3. Видны записи:
   - Категория «Склад», тип MATERIAL_CREATED — создание «Фанера берёзовая»
   - Категория «Склад», тип MATERIAL_UPDATED — переименование
   - Категория «Склад», тип MATERIAL_DELETED — удаление «Краска белая»
   - Категория «Склад», тип STOCK_RECEIPT — приход 100 м² фанеры
   - Категория «Склад», тип STOCK_RECEIPT — приход 50.5 м² фанеры

## API Smoke Test

```bash
# Login
TOKEN=$(curl -s http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | jq -r .token)

# Create material
curl -s -X POST http://localhost:8080/api/materials \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Тестовый материал","unit":"PIECE"}' | jq .

# List materials
curl -s http://localhost:8080/api/materials \
  -H "Authorization: Bearer $TOKEN" | jq .

# Receipt
MATERIAL_ID=$(curl -s http://localhost:8080/api/materials \
  -H "Authorization: Bearer $TOKEN" | jq -r '.items[0].id')

curl -s -X POST "http://localhost:8080/api/materials/$MATERIAL_ID/receipt" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"quantity":42.5,"comment":"Test receipt"}' | jq .

# Check stock updated
curl -s http://localhost:8080/api/materials \
  -H "Authorization: Bearer $TOKEN" | jq '.items[0].currentStock'
# → 42.5

# Movements
curl -s "http://localhost:8080/api/materials/$MATERIAL_ID/movements" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

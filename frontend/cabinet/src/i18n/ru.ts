import type { Messages } from './keys'

export const ru: Messages = {
  app: {
    name: 'Кабинет CTfind',
    tagline: 'Управление заказами',
  },
  nav: {
    dashboard: 'Обзор',
    orders: 'Заказы',
    customers: 'Клиенты',
    logout: 'Выйти',
    openInDesk: 'Открыть в Desk',
  },
  common: {
    loading: 'Загрузка…',
    save: 'Сохранить',
    cancel: 'Отмена',
    delete: 'Удалить',
    create: 'Создать',
    search: 'Поиск',
    refresh: 'Обновить',
    retry: 'Повторить',
    open: 'Открыть',
    close: 'Закрыть',
    yes: 'Да',
    no: 'Нет',
    actions: 'Действия',
    empty: 'Ничего не найдено',
    notFound: 'Страница не найдена',
    forbidden: 'Нет доступа',
    sessionExpired: 'Сессия истекла',
    backendUnavailable: 'Сервер недоступен. Повторите попытку через несколько секунд.',
    minViewport: 'Кабинет рассчитан на экран от 1024 px. Откройте на устройстве с большим экраном.',
  },
  orders: {
    list: {
      title: 'Заказы',
      empty: 'Заказов по выбранным фильтрам нет',
      new: 'Новый заказ',
      search: 'Поиск по номеру или клиенту…',
      filters: 'Фильтры',
    },
    new: {
      title: 'Новый заказ',
      submit: 'Создать заказ',
      cancel: 'Отмена',
    },
    detail: {
      title: 'Заказ',
      history: 'История',
      items: 'Состав',
      info: 'Сводка',
      actions: 'Действия',
    },
    fields: {
      customer: 'Клиент',
      delivery_date: 'Срок исполнения',
      status: 'Статус',
      items: 'Позиции',
      notes: 'Комментарий',
      created_by_staff: 'Создатель',
      modified: 'Изменён',
    },
    items: {
      add: 'Добавить позицию',
      remove: 'Удалить',
      item_name: 'Наименование',
      quantity: 'Количество',
      uom: 'Ед. изм.',
      empty: 'Добавьте хотя бы одну позицию',
    },
    errors: {
      frozen: 'Поле заблокировано в текущем статусе',
      timestamp_mismatch: 'Заказ изменён другим пользователем. Перезагрузите страницу.',
      shipped_readonly: 'Заказ отгружен — редактирование недоступно',
      no_items: 'В заказе должна быть хотя бы одна позиция',
      generic_save: 'Не удалось сохранить заказ',
    },
    conflict: {
      title: 'Заказ изменён другим пользователем',
      description:
        'Чтобы избежать перезаписи чужих изменений, выберите дальнейшее действие: перезагрузить страницу с актуальной версией (ваши правки в этой вкладке потеряются) или открыть заказ в новой вкладке для ручного переноса данных.',
      reload: 'Перезагрузить',
      open_copy: 'Открыть копию',
    },
    workflow: {
      transition: 'Перевести в статус',
      success: 'Статус успешно изменён',
      failed: 'Не удалось изменить статус',
    },
  },
  customers: {
    list: { title: 'Клиенты', empty: 'Клиенты не найдены', new: 'Новый клиент', search: 'Поиск по названию или контакту…' },
    create: { title: 'Новый клиент', submit: 'Создать клиента' },
    fields: { name: 'Наименование', contact_person: 'Контакт', phone: 'Телефон', email: 'Email', status: 'Статус' },
  },
  ui: {
    appearance: 'Внешний вид',
    theme: 'Тема',
    themeOptions: {
      dark: 'Тёмная',
      light: 'Светлая',
    },
    sidebarPreset: 'Цвет сайдбара',
    sidebarPresets: {
      none: 'Без градиента',
      ocean: 'Океан',
      sunset: 'Закат',
      forest: 'Лес',
      twilight: 'Сумерки',
      graphite: 'Графит',
    },
  },
  dashboard: {
    title: 'Дашборд',
    subtitle: 'Обзор активных заказов',
    kpi: {
      totalActive: 'Всего активных',
      inProgress: 'В работе',
      ready: 'Готовы к отгрузке',
      overdue: 'Просроченные',
      trendUp: 'рост',
      trendDown: 'снижение',
      trendNeutral: 'без изменений',
    },
    trendChart: {
      title: 'Динамика заказов за 30 дней',
      empty: 'За выбранный период нет данных',
      ariaLabelTemplate: 'Динамика заказов: {summary}',
    },
    statusDistribution: {
      title: 'Распределение по статусам',
      empty: 'Нет заказов для отображения',
    },
    recentOrders: {
      title: 'Последние заказы',
      empty: 'Пока нет заказов',
      seeAll: 'Все заказы',
    },
    recentChanges: {
      title: 'Последние изменения статусов',
      empty: 'Пока нет изменений',
      template: '{actor}: {order} перевёл «{from}» → «{to}»',
    },
    emptyAll: 'Пока в системе нет заказов. Создайте первый — и здесь появятся виджеты с метриками.',
    createFirst: 'Создать первый заказ',
    statusFilters: {
      allActive: 'Все активные',
    },
    overdueToggle: 'Только просроченные',
  },
  auth: {
    login: {
      title: 'Вход в Кабинет',
      submit: 'Войти',
      username: 'Логин (email)',
      password: 'Пароль',
      failed: 'Неверный логин или пароль',
    },
    logout: { confirm: 'Выйти из Кабинета?' },
    sessionExpired: {
      title: 'Сессия истекла',
      description:
        'Чтобы продолжить работу, войдите снова. Введённые данные форм сохранятся в текущей вкладке.',
      relogin: 'Войти снова',
      saveDraft: 'Сохранить черновик',
    },
    noModules: {
      title: 'У вас нет доступа к модулям Кабинета',
      description:
        'Свяжитесь с администратором, чтобы получить роль Order Manager или Shop Supervisor.',
    },
    forbidden: {
      title: 'Нет доступа к этой странице',
      description: 'Раздел Кабинета недоступен текущей роли. Вернитесь на главную или обратитесь к администратору.',
    },
  },
  login: {
    title: 'Вход в Кабинет',
    brand: {
      title: 'CTfind',
      subtitle: 'Кабинет',
      org: 'Современные технологии',
    },
    welcome: 'Добро пожаловать!',
    form: {
      username: 'Логин (email)',
      password: 'Пароль',
    },
    action: {
      submit: 'Войти',
    },
    error: {
      invalid: 'Неверный логин или пароль',
      disabled: 'Учётная запись отключена. Обратитесь к администратору',
      rateLimit: 'Слишком много попыток. Попробуйте через несколько минут',
      twoFa: 'Для вашей учётной записи требуется двухфакторная аутентификация. Войдите через Frappe Desk',
      network: 'Не удалось связаться с сервером. Проверьте соединение и попробуйте ещё раз',
      empty: 'Заполните поле',
      unavailable: 'Авторизация временно недоступна. Повторите попытку позже',
    },
    notice: {
      sessionExpired: 'Сессия истекла. Войдите снова',
      capsLock: 'Включён Caps Lock',
    },
    noscript: 'Кабинет требует JavaScript. Используйте стандартную форму входа Frappe',
  },
  // 010-cabinet-layout-rework: заголовки страниц (route.meta.title → i18n).
  meta: {
    title: {
      dashboard: 'Обзор',
      orders: {
        list: 'Заказы',
        new: 'Новый заказ',
        detail: 'Заказ',
      },
      customers: {
        list: 'Клиенты',
      },
      forbidden: 'Нет доступа',
      notFound: 'Страница не найдена',
      noModules: 'Нет модулей',
      login: 'Вход',
    },
  },
  sidebar: {
    expand: 'Развернуть меню',
    collapse: 'Свернуть меню',
    version: 'v {version}',
    brand: {
      captionTop: 'ПАНЕЛЬ',
      captionBottom: 'КАБИНЕТА',
      alt: 'CTfind — производственный контроль',
    },
  },
  layout: {
    back: 'Назад',
    backAria: 'Вернуться на предыдущую страницу',
  },
}

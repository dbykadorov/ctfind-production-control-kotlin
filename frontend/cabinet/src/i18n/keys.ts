/**
 * Type-safe ключи для vue-i18n. Структура повторяется в `ru.ts`.
 * Никогда не хранить здесь статусы заказов: они приходят из БД (см. R-010).
 */

export interface Messages {
  app: {
    name: string
    tagline: string
  }
  nav: {
    dashboard: string
    orders: string
    productionTasks: string
    productionTasksBoard: string
    customers: string
    audit: string
    logout: string
    openInDesk: string
  }
  ui: {
    appearance: string
    theme: string
    themeOptions: {
      dark: string
      light: string
    }
    sidebarPreset: string
    sidebarPresets: {
      // 010-cabinet-layout-rework §R-008: solid фон var(--bg-app), default для новых
      // пользователей. Sidebar в этом режиме сливается с body (FR-002).
      none: string
      ocean: string
      sunset: string
      forest: string
      twilight: string
      graphite: string
    }
  }
  dashboard: {
    title: string
    subtitle: string
    kpi: {
      totalActive: string
      inProgress: string
      ready: string
      overdue: string
      trendUp: string
      trendDown: string
      trendNeutral: string
    }
    trendChart: {
      title: string
      empty: string
      ariaLabelTemplate: string
    }
    statusDistribution: {
      title: string
      empty: string
    }
    recentOrders: {
      title: string
      empty: string
      seeAll: string
    }
    recentChanges: {
      title: string
      empty: string
      template: string
    }
    emptyAll: string
    createFirst: string
    statusFilters: {
      allActive: string
    }
    overdueToggle: string
  }
  common: {
    loading: string
    save: string
    cancel: string
    delete: string
    create: string
    search: string
    refresh: string
    retry: string
    open: string
    close: string
    yes: string
    no: string
    actions: string
    empty: string
    notFound: string
    forbidden: string
    sessionExpired: string
    backendUnavailable: string
    minViewport: string
  }
  orders: {
    list: { title: string, empty: string, new: string, search: string, filters: string }
    new: { title: string, submit: string, cancel: string }
    detail: { title: string, history: string, items: string, info: string, actions: string }
    fields: {
      customer: string
      delivery_date: string
      status: string
      items: string
      notes: string
      created_by_staff: string
      modified: string
    }
    items: {
      add: string
      remove: string
      item_name: string
      quantity: string
      uom: string
      empty: string
    }
    errors: {
      frozen: string
      timestamp_mismatch: string
      shipped_readonly: string
      no_items: string
      generic_save: string
    }
    conflict: {
      title: string
      description: string
      reload: string
      open_copy: string
    }
    workflow: {
      transition: string
      success: string
      failed: string
    }
  }
  customers: {
    list: { title: string, empty: string, new: string, search: string }
    create: { title: string, submit: string }
    fields: { name: string, contact_person: string, phone: string, email: string, status: string }
  }
  auth: {
    login: { title: string, submit: string, username: string, password: string, failed: string }
    logout: { confirm: string }
    sessionExpired: {
      title: string
      description: string
      relogin: string
      saveDraft: string
    }
    noModules: { title: string, description: string }
    forbidden: { title: string, description: string }
  }
  /**
   * Кастомная форма входа Кабинета (фича 009-cabinet-custom-login).
   * Отдельный namespace, чтобы не пересекаться с устаревшими `auth.login.*`
   * (которые остаются для совместимости с фичей 006).
   */
  login: {
    title: string
    /**
     * 010 (login PAM-rework): бренд-блок в заголовке login-карточки.
     * `brand.title` — крупная надпись (CTfind), `brand.subtitle` — подпись под ней
     * (Кабинет / Cabinet), `brand.org` — alt-текст у логотипа «Современные технологии»
     * в верхнем углу страницы.
     */
    brand: {
      title: string
      subtitle: string
      org: string
    }
    /** «Добро пожаловать!» — приветствие над формой. */
    welcome: string
    form: {
      username: string
      password: string
    }
    action: {
      submit: string
    }
    error: {
      invalid: string
      disabled: string
      rateLimit: string
      twoFa: string
      network: string
      empty: string
      unavailable: string
    }
    notice: {
      sessionExpired: string
      capsLock: string
    }
    noscript: string
  }
  /**
   * 010-cabinet-layout-rework — заголовки для TopBar (route.meta.title → i18n key).
   * Каждый key соответствует одному route.name (см. router/index.ts, T013).
   * Sub-namespace `orders.*` дублирует структуру router (orders.list/orders.new/...).
   */
  meta: {
    title: {
      dashboard: string
      orders: {
        list: string
        new: string
        detail: string
      }
      customers: {
        list: string
      }
      productionTasks: {
        list: string
        detail: string
        board: string
      }
      audit: string
      forbidden: string
      notFound: string
      noModules: string
      login: string
    }
  }
  /**
   * 010 — Sidebar-специфичные строки (collapse-toggle aria-label, версия).
   */
  sidebar: {
    expand: string
    collapse: string
    /** Шаблон "v {version}" — render через t('sidebar.version', { version: '0.1.0' }). */
    version: string
    /**
     * 010 (PAM-rework, post-MVP): бренд-блок в header sidebar.
     * `brand.captionTop` — мелкий caption над названием продукта (ПАНЕЛЬ / PANEL).
     * `brand.captionBottom` — крупный caption (КАБИНЕТ / CABINET).
     * `brand.alt` — alt-текст для логотипа-знака CT в header.
     */
    brand: {
      captionTop: string
      captionBottom: string
      alt: string
    }
  }
  /**
   * 010 — Layout-специфичные строки (BackButton, общие ярлыки).
   */
  layout: {
    /** Текст title-атрибута кнопки «Назад» (видим при hover). */
    back: string
    /** aria-label кнопки «Назад» для screen-reader'ов. */
    backAria: string
  }
  audit: {
    refresh: string
    resetFilters: string
    filters: {
      dateFrom: string
      dateTo: string
      category: string
      actor: string
      search: string
    }
    columns: {
      time: string
      category: string
      eventType: string
      actor: string
      summary: string
      target: string
    }
    category: {
      AUTH: string
      ORDER: string
      PRODUCTION_TASK: string
    }
    empty: string
    emptyFiltered: string
    errorLoading: string
    forbidden: string
    totalItems: string
    page: string
  }
}

export type MessagePath =
  | 'app.name'
  | 'nav.dashboard'
  | 'nav.orders'
  | 'orders.list.title'

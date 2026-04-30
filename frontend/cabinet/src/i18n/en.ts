/**
 * English locale for the Cabinet (009-cabinet-custom-login).
 *
 * NOTE: this is a *partial* locale — only the strings introduced by feature
 * 009 (the custom login form) are translated here. All other namespaces fall
 * back to `ru` via `fallbackLocale: 'ru'` in i18n/index.ts. As features add
 * English support, they may extend this object.
 */

export const en = {
  login: {
    title: 'Sign in to Cabinet',
    brand: {
      title: 'CTfind',
      subtitle: 'Cabinet',
      org: 'Modern Technologies',
    },
    welcome: 'Welcome!',
    form: {
      username: 'Username (email)',
      password: 'Password',
    },
    action: {
      submit: 'Sign in',
    },
    error: {
      invalid: 'Invalid login or password',
      disabled: 'This account is disabled. Please contact your administrator',
      rateLimit: 'Too many attempts. Please try again in a few minutes',
      twoFa: 'Your account requires two-factor authentication. Please contact an administrator',
      network: 'Could not reach the server. Check your connection and try again',
      empty: 'This field is required',
      unavailable: 'Authorization is temporarily unavailable. Please try again later',
    },
    notice: {
      sessionExpired: 'Your session has expired. Please sign in again',
      capsLock: 'Caps Lock is on',
    },
    noscript: 'Cabinet requires JavaScript. Please enable JavaScript and reload the page',
  },
  // 010-cabinet-layout-rework: page titles for TopBar (route.meta.title → i18n key).
  meta: {
    title: {
      dashboard: 'Overview',
      orders: {
        list: 'Orders',
        new: 'New order',
        detail: 'Order',
      },
      customers: {
        list: 'Customers',
      },
      users: {
        list: 'Users',
      },
      productionTasks: {
        list: 'Production tasks',
        detail: 'Production task',
        board: 'Production tasks board',
      },
      audit: 'Audit Log',
      forbidden: 'Access denied',
      notFound: 'Page not found',
      noModules: 'No modules available',
      login: 'Sign in',
    },
  },
  sidebar: {
    expand: 'Expand menu',
    collapse: 'Collapse menu',
    version: 'v {version}',
    brand: {
      captionTop: 'PANEL',
      captionBottom: 'CABINET',
      alt: 'CTfind — Production control',
    },
  },
  nav: {
    audit: 'Audit Log',
    users: 'Users',
  },
  users: {
    title: 'Users',
    subtitle: 'Manage users and access roles',
    search: 'Search by login or name…',
    create: 'Create user',
    empty: 'No users found',
    forbidden: 'No access to users section',
    fields: {
      login: 'Login',
      displayName: 'Display name',
      initialPassword: 'Initial password',
      roles: 'Roles',
    },
    actions: {
      submit: 'Create',
      cancel: 'Cancel',
      refresh: 'Refresh',
    },
    messages: {
      created: 'User created',
      duplicate: 'User login already exists',
      validation: 'Please check form fields',
      invalidRoles: 'Selected roles are not allowed',
      forbidden: 'You do not have permission',
      generic: 'Operation failed',
    },
  },
  layout: {
    back: 'Back',
    backAria: 'Return to the previous page',
  },
  audit: {
    refresh: 'Refresh',
    resetFilters: 'Reset',
    filters: {
      dateFrom: 'Date from',
      dateTo: 'Date to',
      category: 'Category',
      actor: 'Actor',
      search: 'Search by description, object ID, or login',
    },
    columns: {
      time: 'Time',
      category: 'Category',
      eventType: 'Event',
      actor: 'Who',
      summary: 'Description',
      target: '',
    },
    category: {
      AUTH: 'Authentication',
      ORDER: 'Orders',
      PRODUCTION_TASK: 'Production',
    },
    empty: 'No events for the selected period',
    emptyFiltered: 'No events match the filters — change or reset them',
    errorLoading: 'Failed to load audit log',
    forbidden: 'No access to audit log',
    totalItems: 'Total: {count} events',
    page: 'Page {current} of {total}',
  },
}

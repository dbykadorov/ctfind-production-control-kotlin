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
  layout: {
    back: 'Back',
    backAria: 'Return to the previous page',
  },
}

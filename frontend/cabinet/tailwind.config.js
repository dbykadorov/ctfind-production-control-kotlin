/** @type {import('tailwindcss').Config} */
export default {
  /**
   * 007: Тёмная тема — дефолт; CSS-переменные в tokens.css сами адаптируются по
   * data-theme на <html>. Tailwind dark:-вариант триггерим на data-theme='dark',
   * но в коде он почти не нужен (используем семантические токены bg/surface/ink).
   *
   * 010: Добавлены layout-токены (--bg-app, --card-radius, --sidebar-width-*,
   * --header-height, --topbar-icon-size, --transition-base) — мост в Tailwind
   * через расширенные ключи `colors.app.bg`, `borderRadius.card`, `width.sidebar.*`,
   * `height.header`, `transitionDuration.base`.
   */
  darkMode: ['class', '[data-theme="dark"]'],
  content: ['./index.html', './src/**/*.{vue,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: 'var(--c-brand-50)',
          100: 'var(--c-brand-100)',
          500: 'var(--c-brand-500)',
          600: 'var(--c-brand-600)',
          700: 'var(--c-brand-700)',
        },
        bg: 'var(--c-bg)',
        surface: 'var(--c-surface)',
        elevated: 'var(--c-elevated)',
        overlay: 'var(--c-overlay)',
        // 010: новый семантический алиас для viewport-фона.
        // app: { bg } даёт `bg-app-bg`, `text-app-bg`, etc.
        app: {
          bg: 'var(--bg-app)',
        },
        ink: {
          strong: 'var(--c-fg-strong)',
          DEFAULT: 'var(--c-fg)',
          muted: 'var(--c-fg-muted)',
          inverse: 'var(--c-fg-inverse)',
        },
        border: {
          DEFAULT: 'var(--c-border)',
          strong: 'var(--c-border-strong)',
        },
        status: {
          new: 'var(--c-status-new)',
          progress: 'var(--c-status-progress)',
          ready: 'var(--c-status-ready)',
          shipped: 'var(--c-status-shipped)',
        },
        danger: 'var(--c-danger)',
        warning: 'var(--c-warning)',
        success: 'var(--c-success)',
      },
      fontFamily: {
        sans: ['var(--font-sans)'],
        mono: ['var(--font-mono)'],
      },
      borderRadius: {
        sm: 'var(--radius-sm)',
        DEFAULT: 'var(--radius)',
        lg: 'var(--radius-lg)',
        // 010: floating-card радиус.
        card: 'var(--card-radius)',
      },
      boxShadow: {
        'card': 'var(--shadow-card)',
        'elevated': 'var(--shadow-elevated)',
        // 010: тень для floating-card.
        'cabinet-card': 'var(--card-shadow)',
      },
      transitionTimingFunction: {
        'out-expo': 'var(--ease-out)',
      },
      transitionDuration: {
        DEFAULT: 'var(--duration)',
        // 010: длительность sidebar collapse/expand.
        base: 'var(--transition-base-duration, 350ms)',
      },
      // 010: фиксированные размеры layout (sidebar collapse/expand, topbar).
      width: {
        'sidebar-expanded': 'var(--sidebar-width-expanded)',
        'sidebar-collapsed': 'var(--sidebar-width-collapsed)',
      },
      minWidth: {
        'sidebar-expanded': 'var(--sidebar-width-expanded)',
        'sidebar-collapsed': 'var(--sidebar-width-collapsed)',
      },
      height: {
        'header': 'var(--header-height)',
        'topbar-icon': 'var(--topbar-icon-size)',
      },
      minHeight: {
        header: 'var(--header-height)',
      },
      spacing: {
        'card-margin': 'var(--card-margin)',
      },
    },
  },
  plugins: [],
}

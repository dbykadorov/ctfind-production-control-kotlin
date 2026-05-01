<script setup lang="ts">
/**
 * Sidebar — навигационная панель Кабинета (фичи 006/007/010).
 *
 * 010-cabinet-layout-rework:
 *   - US1: floating-card model — sidebar по умолчанию `transparent`
 *     (фон наследуется от .cabinet-shell градиента); gradient-presets фичи 007
 *     остаются как user-override через .cabinet-sidebar-bg.
 *   - US2: collapse/expand с transition по `--transition-base` (350ms);
 *     каждый пункт меню — отдельный <SidebarItem>, который сам показывает
 *     tooltip справа от иконки в свёрнутом состоянии (WAI-ARIA pattern).
 *   - US4 (post-MVP): версия SPA в footer — lazy-show через 300ms,
 *     fade-out при collapse (см. SidebarItem fade pattern).
 *   - PAM-rework (post-MVP): header содержит SVG-знак CTfind + caption-блок;
 *     footer содержит круглую chevron-кнопку коллапса (поворот 180° при collapse)
 *     + версию SPA. См. эталон PAM `services/frontend/uc/components/SideBar`.
 *
 * Активный пункт высчитывается здесь (родитель), передаётся в SidebarItem
 * через :active prop — компонент пункта только рендерит и обрабатывает hover/focus.
 */
import {
  Bell,
  ChevronLeft,
  ClipboardList,
  Factory,
  LayoutDashboard,
  LayoutGrid,
  Package,
  ScrollText,
  Users,
} from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { usePermissions } from '@/api/composables/use-permissions'
import logoMarkCt from '@/assets/sidebar/logo-mark-ct.svg'
import { cn } from '@/lib/utils'
import { useUiStore } from '@/stores/ui'
import SidebarItem from './SidebarItem.vue'

const route = useRoute()
const ui = useUiStore()
const permissions = usePermissions()
const { t } = useI18n()

interface NavItem {
  to: string
  icon: typeof LayoutDashboard
  key: string
  visible: boolean
}

const items = computed<NavItem[]>(() => [
  {
    to: '/cabinet',
    icon: LayoutDashboard,
    key: 'nav.dashboard',
    visible: true,
  },
  {
    to: '/cabinet/orders',
    icon: ClipboardList,
    key: 'nav.orders',
    visible: permissions.value.canViewOrderBom,
  },
  {
    to: '/cabinet/production-tasks',
    icon: Factory,
    key: 'nav.productionTasks',
    visible:
      permissions.value.canViewAllProductionTasks
      || permissions.value.canWorkAssignedProductionTasks,
  },
  {
    to: '/cabinet/production-tasks/board',
    icon: LayoutGrid,
    key: 'nav.productionTasksBoard',
    visible:
      permissions.value.canViewAllProductionTasks
      || permissions.value.canWorkAssignedProductionTasks,
  },
  {
    to: '/cabinet/customers',
    icon: Users,
    key: 'nav.customers',
    visible: permissions.value.canManageCustomers,
  },
  {
    to: '/cabinet/users',
    icon: Users,
    key: 'nav.users',
    visible: permissions.value.isAdmin,
  },
  {
    to: '/cabinet/notifications',
    icon: Bell,
    key: 'nav.notifications',
    visible: true,
  },
  {
    to: '/cabinet/warehouse',
    icon: Package,
    key: 'nav.warehouse',
    visible: permissions.value.isWarehouse || permissions.value.isAdmin,
  },
  {
    to: '/cabinet/audit',
    icon: ScrollText,
    key: 'nav.audit',
    visible: permissions.value.isAdmin,
  },
])

function isActive(to: string): boolean {
  if (to === '/cabinet')
    return route.path === '/cabinet' || route.path === '/cabinet/'
  return route.path.startsWith(to)
}

// US4 (010, FR-029..FR-032): lazy-show версии SPA через 300ms — избегаем layout-shift
// на первом рендере sidebar (footer "схлопывается" с чистой высотой, потом плавно
// появляется текст). При collapse версия fade-out через CSS opacity-transition.
const versionReady = ref(false)
onMounted(() => {
  setTimeout(() => {
    versionReady.value = true
  }, 300)
})

const versionText = computed(() =>
  t('sidebar.version', { version: __APP_VERSION__ }),
)
</script>

<template>
  <aside
    :class="
      cn(
        'cabinet-sidebar flex h-full shrink-0 flex-col',
        ui.sidebarPreset !== 'none' && 'cabinet-sidebar-bg',
        ui.sidebarCollapsed ? 'w-sidebar-collapsed' : 'w-sidebar-expanded',
      )
    "
  >
    <!-- 010 PAM-rework: header — лого-знак + caption-блок (ПАНЕЛЬ / КАБИНЕТА). -->
    <div class="cabinet-sidebar__header flex h-header items-center gap-4 px-5">
      <img
        :src="logoMarkCt"
        :alt="t('sidebar.brand.alt')"
        class="cabinet-sidebar__logo size-10 shrink-0"
        width="40"
        height="40"
      >
      <div
        class="cabinet-sidebar__caption flex flex-col leading-tight"
        :class="{ 'cabinet-sidebar__caption--hidden': ui.sidebarCollapsed }"
      >
        <span
          class="cabinet-sidebar__caption-top text-[11px] font-normal uppercase tracking-wider"
        >
          {{ t("sidebar.brand.captionTop") }}
        </span>
        <span
          class="cabinet-sidebar__caption-bottom text-base font-bold uppercase tracking-wide"
        >
          {{ t("sidebar.brand.captionBottom") }}
        </span>
      </div>
    </div>

    <!-- Тонкая разделительная линия под header (как в PAM `sidebar__header-line`). -->
    <div
      class="cabinet-sidebar__divider mx-5 h-px shrink-0"
      aria-hidden="true"
    />

    <nav class="flex flex-col gap-0.5 p-2 pt-3">
      <template v-for="item in items" :key="item.to">
        <SidebarItem
          v-if="item.visible"
          :to="item.to"
          :icon="item.icon"
          :label-key="item.key"
          :active="isActive(item.to)"
        />
      </template>
    </nav>

    <!-- 010 PAM-rework: footer — chevron-collapse-кнопка + версия SPA.
         mt-auto прижимает footer к низу, сохраняя scroll-зону под навигацию. -->
    <div
      class="cabinet-sidebar__footer mt-auto flex items-center justify-between px-5 py-5"
    >
      <button
        type="button"
        class="cabinet-sidebar__toggle"
        :class="{ 'cabinet-sidebar__toggle--collapsed': ui.sidebarCollapsed }"
        :aria-label="
          ui.sidebarCollapsed ? t('sidebar.expand') : t('sidebar.collapse')
        "
        @click="ui.toggleSidebar()"
      >
        <ChevronLeft class="size-5" aria-hidden="true" />
      </button>
      <span
        v-if="versionReady"
        class="cabinet-sidebar__version text-xs font-normal"
        :class="{ 'cabinet-sidebar__version--hidden': ui.sidebarCollapsed }"
        data-testid="sidebar-version"
      >
        {{ versionText }}
      </span>
    </div>
  </aside>
</template>

<style scoped>
/* US2: smooth ширина-transition при collapse/expand. Длительность из дизайн-токена. */
.cabinet-sidebar {
  transition: width var(--transition-base);
  will-change: width;
}

/* PAM-rework: caption-блок и версия плавно скрываются при collapse
   (как `.menu__item-text` fade-pattern, но в bordered контексте header/footer). */
.cabinet-sidebar__caption,
.cabinet-sidebar__version {
  transition:
    opacity var(--sidebar-label-fade),
    transform var(--sidebar-label-fade);
  transform: translateX(0);
  white-space: nowrap;
}

.cabinet-sidebar__caption--hidden,
.cabinet-sidebar__version--hidden {
  opacity: 0;
  transform: translateX(20px);
  pointer-events: none;
}

.cabinet-sidebar__caption-top {
  color: var(--sidebar-caption-muted);
}

.cabinet-sidebar__caption-bottom {
  color: var(--sidebar-fg-strong);
}

.cabinet-sidebar__divider {
  background: var(--sidebar-divider);
}

.cabinet-sidebar__version {
  color: var(--sidebar-caption-soft);
}

/* PAM-rework: круглая chevron-кнопка коллапса. По умолчанию (expanded)
   стрелка указывает влево; при collapse — поворот на 180° через transform.
   Postfix-полировка: добавлен subtle-фон в idle-состоянии и тонкая граница,
   чтобы кнопка читалась на любом стопе градиента (верх/низ sidebar). */
.cabinet-sidebar__toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 1px solid var(--sidebar-toggle-border);
  background: var(--sidebar-toggle-bg);
  color: var(--sidebar-toggle-fg);
  cursor: pointer;
  border-radius: 9999px;
  transition:
    transform var(--transition-base),
    background-color var(--duration) var(--ease-out),
    border-color var(--duration) var(--ease-out),
    color var(--duration) var(--ease-out);
}

.cabinet-sidebar__toggle:hover {
  background: var(--sidebar-toggle-bg-hover);
  border-color: var(--sidebar-toggle-border-hover);
  color: var(--sidebar-fg-strong);
}

.cabinet-sidebar__toggle:focus-visible {
  outline: 2px solid var(--c-brand-500);
  outline-offset: 2px;
}

.cabinet-sidebar__toggle--collapsed {
  transform: rotate(180deg);
}
</style>

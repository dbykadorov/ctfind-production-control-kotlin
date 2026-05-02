<script setup lang="ts">
/**
 * SidebarItem — пункт меню Sidebar Кабинета (010-cabinet-layout-rework, US2).
 *
 * Контракт: specs/010-cabinet-layout-rework/contracts/sidebar-tooltip.contract.md
 *
 * В свёрнутом sidebar (`useUiStore.sidebarCollapsed === true`) при `mouseenter`
 * или `focus` показывает floating tooltip справа от иконки.
 * Соответствует WAI-ARIA Tooltip Pattern.
 *
 * Ключевые гарантии:
 *   - ST-G1: tooltip визуально скрыт при развёрнутом sidebar (v-show)
 *   - ST-G4: aria-labelledby всегда указывает на tooltip-id в свёрнутом состоянии,
 *            tooltip остаётся в DOM (v-show, не v-if), чтобы скринридер мог его
 *            прочитать в любой момент
 *   - ST-G7: auto-shift по нижнему краю viewport
 */
import { type Component, computed, ref, useId } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { useUiStore } from '@/stores/ui'

interface SidebarItemProps {
  to: string
  icon: Component
  labelKey: string
  active: boolean
}

defineProps<SidebarItemProps>()

const ui = useUiStore()
const { t } = useI18n()

const tooltipId = `tt-${useId()}`
const tooltipVisible = ref(false)
const tooltipPosition = ref<{ top: string, left: string, transform?: string }>({
  top: '0',
  left: '0',
})

// ref на <RouterLink> возвращает экземпляр компонента, а не DOM-узел.
// Для getBoundingClientRect нужен `.$el` — фактический <a>.
const linkRef = ref<{ $el?: HTMLElement } | HTMLElement | null>(null)

const collapsed = computed(() => ui.sidebarCollapsed)

const TOOLTIP_HEIGHT = 32

function resolveEl(): HTMLElement | null {
  const r = linkRef.value
  if (!r)
    return null
  if ('$el' in r && r.$el instanceof HTMLElement)
    return r.$el
  if (r instanceof HTMLElement)
    return r
  return null
}

function show() {
  if (!collapsed.value)
    return
  const el = resolveEl()
  if (!el)
    return
  const rect = el.getBoundingClientRect()
  const left = `${rect.right + 8}px`
  const proposedTop = rect.top + rect.height / 2
  const viewportH = typeof window !== 'undefined' ? window.innerHeight : 1080
  if (proposedTop + TOOLTIP_HEIGHT / 2 > viewportH - 8) {
    tooltipPosition.value = {
      left,
      top: `${viewportH - TOOLTIP_HEIGHT - 8}px`,
      transform: 'none',
    }
  }
  else {
    tooltipPosition.value = {
      left,
      top: `${proposedTop}px`,
    }
  }
  tooltipVisible.value = true
}

function hide() {
  tooltipVisible.value = false
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && tooltipVisible.value)
    hide()
}

const tooltipStyle = computed<Record<string, string>>(() => ({
  top: tooltipPosition.value.top,
  left: tooltipPosition.value.left,
  ...(tooltipPosition.value.transform ? { transform: tooltipPosition.value.transform } : {}),
}))
</script>

<template>
  <RouterLink
    ref="linkRef"
    :to="to"
    class="cabinet-sidebar-item flex h-10 items-center gap-3 rounded px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
    :class="{ 'cabinet-sidebar-item--active font-semibold': active }"
    :aria-current="active ? 'page' : undefined"
    :aria-labelledby="collapsed ? tooltipId : undefined"
    @mouseenter="show"
    @mouseleave="hide"
    @focus="show"
    @blur="hide"
    @keydown="onKeydown"
  >
    <component :is="icon" class="size-5 shrink-0" aria-hidden="true" />
    <span
      class="cabinet-sidebar-item__label truncate"
      :class="{ 'cabinet-sidebar-item__label--hidden': collapsed }"
    >
      {{ t(labelKey) }}
    </span>
  </RouterLink>

  <Teleport to="body">
    <div
      v-show="tooltipVisible && collapsed"
      :id="tooltipId"
      role="tooltip"
      class="cabinet-tooltip"
      :style="tooltipStyle"
    >
      {{ t(labelKey) }}
    </div>
  </Teleport>
</template>

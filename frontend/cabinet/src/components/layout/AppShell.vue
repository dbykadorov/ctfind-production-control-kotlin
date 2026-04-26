<script setup lang="ts">
/**
 * AppShell — корневой layout Кабинета (010-cabinet-layout-rework, US1).
 *
 * Структура (floating-card model по образцу PAM UC):
 *
 *   .cabinet-shell  (navy/light viewport-фон var(--bg-app))
 *   ├── <Sidebar>  (сливается с фоном при preset='none', либо gradient-override)
 *   └── .cabinet-card  (белая card с радиусом и тенью)
 *       ├── <TopBar>  (header-height var)
 *       └── <main>  (overflow-y-auto, RouterView с Transition)
 *
 * См. specs/010-cabinet-layout-rework/spec.md US1, contracts/design-tokens.contract.md,
 * research.md §R-004 (scoped fg-перебивка внутри .cabinet-card для контраста).
 */
import { RouterView } from 'vue-router'
import Sidebar from './Sidebar.vue'
import TopBar from './TopBar.vue'
import UnsupportedViewport from './UnsupportedViewport.vue'
</script>

<template>
  <div class="cabinet-shell flex h-screen w-screen overflow-hidden bg-app-bg text-ink">
    <Sidebar />
    <div class="cabinet-card m-card-margin flex min-w-0 flex-1 flex-col">
      <TopBar />
      <main class="flex-1 overflow-y-auto p-6">
        <RouterView v-slot="{ Component, route }">
          <Transition name="cabinet-fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </Transition>
        </RouterView>
      </main>
    </div>
    <UnsupportedViewport />
  </div>
</template>

<style>
.cabinet-fade-enter-from,
.cabinet-fade-leave-to {
  opacity: 0;
  transform: translateY(2px);
}
.cabinet-fade-enter-active,
.cabinet-fade-leave-active {
  transition:
    opacity var(--duration) var(--ease-out),
    transform var(--duration) var(--ease-out);
}
</style>

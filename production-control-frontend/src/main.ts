/**
 * Точка входа SPA Кабинета.
 * Standalone Vite runtime ожидает `<div id="app">` в `index.html`.
 */

import { createPinia } from 'pinia'
import { createApp } from 'vue'
import App from './App.vue'
import { i18n } from './i18n'
import { router } from './router'
import { useAuthStore } from './stores/auth'
import './styles/globals.css'

async function bootstrap(): Promise<void> {
  const app = createApp(App)
  const pinia = createPinia()
  app.use(pinia)
  await useAuthStore().bootstrapFromStoredToken()
  app.use(router)
  app.use(i18n)

  app.config.errorHandler = (err, _instance, info) => {
    console.error('[cabinet] errorHandler', err, info)
  }
  app.config.performance = import.meta.env.DEV

  app.mount('#app')
}

void bootstrap()

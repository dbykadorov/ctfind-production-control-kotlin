/**
 * Точка входа SPA Кабинета.
 * Standalone Vite runtime ожидает `<div id="app">` в `index.html`.
 */

import { createPinia } from 'pinia'
import { createApp } from 'vue'
import App from './App.vue'
import { i18n } from './i18n'
import { router } from './router'
import './styles/globals.css'

function bootstrap(): void {
  const app = createApp(App)
  app.use(createPinia())
  app.use(router)
  app.use(i18n)

  app.config.errorHandler = (err, _instance, info) => {
    console.error('[cabinet] errorHandler', err, info)
  }
  app.config.performance = import.meta.env.DEV

  app.mount('#app')
}

bootstrap()

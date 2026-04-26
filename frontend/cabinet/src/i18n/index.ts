import { createI18n } from 'vue-i18n'
import { en } from './en'
import { ru } from './ru'

export const i18n = createI18n<false>({
  legacy: false,
  globalInjection: true,
  locale: 'ru',
  fallbackLocale: 'ru',
  // vue-i18n ожидает рекурсивный LocaleMessage; наш строгий Messages — его подмножество.
  // EN — частичная локаль (009-cabinet-custom-login): покрывает только новые login.*
  // ключи; всё остальное падает в fallback на ru.
  messages: { ru, en } as unknown as Record<string, Record<string, string>>,
  warnHtmlMessage: false,
  missingWarn: import.meta.env.DEV,
  fallbackWarn: import.meta.env.DEV,
})

export type AppI18n = typeof i18n

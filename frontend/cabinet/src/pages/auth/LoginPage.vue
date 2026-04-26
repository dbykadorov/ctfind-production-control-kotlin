<script setup lang="ts">
import type { ComponentPublicInstance } from 'vue'
/**
 * Кастомная форма входа Кабинета (009-cabinet-custom-login + 010-login PAM rework).
 *
 * 010 visual rework:
 *   - Floating dark card с backdrop-blur поверх navy-фона + декоративный SVG-паттерн.
 *   - Логотип «Современные технологии» в верхнем-левом углу страницы.
 *   - Бренд-блок «CTfind / Кабинет» + welcome-заголовок над формой.
 *   - Outlined-кнопка submit без filled-фона (амбер-glow на hover).
 *   - Принудительно тёмная тема: `data-theme="dark"` локально на корне страницы,
 *     не зависит от глобальной темы пользователя (всегда выглядит как PAM-эталон).
 *
 * Контракты (009):
 *   - data-model.md §2.2 (state machine, query-параметры)
 *   - cabinet-login-contract.md §C-1 (HTTP)
 *   - research.md §R-007 (UX-паттерны)
 *
 * Фича явно НЕ делает: 2FA flow, регистрацию, custom-recovery
 * (см. spec Out of Scope).
 */
import type { LoginErrorKey } from '@/api/auth-service'
import { computed, nextTick, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import logoModtechEn from '@/assets/auth/logo-modtech-en.svg'
import logoModtechRu from '@/assets/auth/logo-modtech-ru.svg'
import bgPattern from '@/assets/auth/sign-up.svg'
import { Input, Label } from '@/components/ui'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const { t, locale } = useI18n()

/** Email/login. */
const usr = ref('')
/** Plaintext пароль. */
const pwd = ref('')
/** Submit-in-flight. Блокирует кнопку. */
const isSubmitting = ref(false)
/** Текущая ошибка (i18n-ключ из `login.error.<key>`) или null. */
const errorKey = ref<LoginErrorKey | null>(null)
/** Включён ли Caps Lock на момент последнего keydown в поле пароля. */
const capsLockOn = ref(false)

/**
 * Ref на компонент Input (а не на нативный <input>). `$el` даёт нам
 * корневой DOM-узел компонента — для Input.vue это и есть `<input>`.
 */
const usernameInput = ref<ComponentPublicInstance | null>(null)

/** Опциональная нотис «сессия истекла» — приходит из ?reason=session-expired. */
const sessionExpiredNotice = computed<boolean>(
  () => route.query.reason === 'session-expired',
)

/** Текст inline-ошибки (или пустая строка для слота фиксированной высоты). */
const errorText = computed<string>(() => {
  if (!errorKey.value)
    return ''
  return t(`login.error.${errorKey.value}`)
})

/** Целевой URL после успешного логина (берём из ?from, очищаем sanitizeFrom'ом в store). */
const targetFrom = computed<string | null>(() => {
  const raw = route.query.from
  return typeof raw === 'string' ? raw : null
})

/** Логотип «Современные технологии» в зависимости от текущей локали. */
const orgLogo = computed<string>(() =>
  locale.value === 'en' ? logoModtechEn : logoModtechRu,
)

function clearError(): void {
  errorKey.value = null
}

function isFormValid(): boolean {
  return usr.value.trim().length > 0 && pwd.value.length > 0
}

async function onSubmit(event?: Event): Promise<void> {
  event?.preventDefault()
  if (isSubmitting.value)
    return

  if (!isFormValid()) {
    errorKey.value = 'empty'
    return
  }

  errorKey.value = null
  isSubmitting.value = true

  try {
    const outcome = await auth.login(usr.value.trim(), pwd.value, targetFrom.value)
    if (outcome.kind === 'success') {
      // Сюда мы не вернёмся — store уже сделал window.location.assign().
      return
    }
    if (outcome.kind === 'two-fa-required') {
      errorKey.value = 'twoFa'
      return
    }
    errorKey.value = outcome.messageKey
  }
  finally {
    isSubmitting.value = false
  }
}

function onPasswordKey(event: KeyboardEvent): void {
  // Caps Lock индикатор (FR-012, T041 в Polish).
  if (typeof event.getModifierState === 'function')
    capsLockOn.value = event.getModifierState('CapsLock')
}

function onPasswordBlur(): void {
  capsLockOn.value = false
}

onMounted(async () => {
  // Авто-фокус на первом поле при монтировании (FR-012, UX best practice).
  // `nextTick` нужен, чтобы DOM Input.vue гарантированно был смонтирован.
  await nextTick()
  const el = usernameInput.value?.$el as HTMLElement | undefined
  el?.focus?.()
})
</script>

<template>
  <main
    class="cabinet-login"
    data-theme="dark"
    @keydown.escape="clearError"
  >
    <!--
      Декоративный SVG-фон (треугольный паттерн из PAM UC).
      aria-hidden — для скрин-ридеров не несёт смысла.
    -->
    <div
      class="cabinet-login__bg"
      :style="{ backgroundImage: `url(${bgPattern})` }"
      aria-hidden="true"
    />

    <!-- Бренд-блок «Современные технологии» в верхнем углу. -->
    <div class="cabinet-login__org">
      <img
        :src="orgLogo"
        :alt="t('login.brand.org')"
        class="cabinet-login__org-img"
        width="240"
        height="57"
      >
    </div>

    <!-- Floating dark card с формой входа. -->
    <section class="cabinet-login__card" aria-labelledby="login-brand-title">
      <header class="cabinet-login__brand">
        <h1 id="login-brand-title" class="cabinet-login__brand-title">
          {{ t('login.brand.title') }}
        </h1>
        <p class="cabinet-login__brand-subtitle">
          {{ t('login.brand.subtitle') }}
        </p>
      </header>

      <h2 class="cabinet-login__welcome">
        {{ t('login.welcome') }}
      </h2>

      <p
        v-if="sessionExpiredNotice"
        class="cabinet-login__notice"
        data-testid="login-notice-session-expired"
        role="status"
      >
        {{ t('login.notice.sessionExpired') }}
      </p>

      <form class="cabinet-login__form" novalidate @submit="onSubmit">
        <div class="cabinet-login__field">
          <Label for="login-username" class="cabinet-login__label">
            {{ t('login.form.username') }}
          </Label>
          <Input
            id="login-username"
            ref="usernameInput"
            v-model="usr"
            type="email"
            autocomplete="username"
            :disabled="isSubmitting"
            :invalid="errorKey === 'empty' && !usr.trim()"
            aria-describedby="login-error"
            class="cabinet-login__input"
            @input="clearError"
          />
        </div>

        <div class="cabinet-login__field">
          <Label for="login-password" class="cabinet-login__label">
            {{ t('login.form.password') }}
          </Label>
          <Input
            id="login-password"
            v-model="pwd"
            type="password"
            autocomplete="current-password"
            :disabled="isSubmitting"
            :invalid="errorKey === 'empty' && !pwd"
            aria-describedby="login-error login-capslock"
            class="cabinet-login__input"
            @input="clearError"
            @keyup="onPasswordKey"
            @keydown="onPasswordKey"
            @blur="onPasswordBlur"
          />
          <p
            v-if="capsLockOn"
            id="login-capslock"
            class="cabinet-login__hint"
            role="status"
          >
            {{ t('login.notice.capsLock') }}
          </p>
        </div>

        <!-- Слот ошибки фиксированной высоты, чтобы layout не «прыгал» при появлении. -->
        <p
          id="login-error"
          data-testid="login-error"
          class="cabinet-login__error"
          role="alert"
          aria-live="polite"
        >
          {{ errorText }}
        </p>

        <button
          type="submit"
          class="cabinet-login__submit"
          :disabled="isSubmitting"
          :aria-busy="isSubmitting || undefined"
        >
          <span
            v-if="isSubmitting"
            class="cabinet-login__submit-spinner"
            aria-hidden="true"
          />
          {{ t('login.action.submit') }}
        </button>
      </form>
    </section>

    <noscript>
      <div class="cabinet-login__noscript">
      {{ t('login.noscript') }}
      <a href="/login" class="underline">/login</a>
      </div>
    </noscript>
  </main>
</template>

<style scoped>
/*
 * 010-login PAM-rework. Все стили scoped — изолируем эту страницу
 * от глобального cabinet-shell. data-theme="dark" на <main> заставляет
 * tokens.css применить dark-вариант CSS-переменных в этом поддереве,
 * независимо от глобальной темы.
 */

.cabinet-login {
  position: relative;
  z-index: 0;
  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  overflow: hidden;
  background: var(--bg-app);
  color: var(--c-fg-strong);
  font-family: var(--font-sans);
}

.cabinet-login__bg {
  position: absolute;
  inset: 0;
  z-index: -1;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.7;
  pointer-events: none;
}

.cabinet-login__org {
  position: absolute;
  top: 32px;
  left: 32px;
  z-index: 1;
  display: flex;
  align-items: center;
}

.cabinet-login__org-img {
  display: block;
  height: 48px;
  width: auto;
}

.cabinet-login__card {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 412px;
  padding: 38px 32px 32px;
  border: 1px solid rgb(255 255 255 / 0.12);
  border-radius: var(--card-radius);
  background: rgb(0 0 0 / 0.4);
  backdrop-filter: saturate(120%) blur(10px);
  -webkit-backdrop-filter: saturate(120%) blur(10px);
  box-shadow:
    0 -2px 5px -1px rgb(26 34 53 / 0.3),
    0 3px 4px 1px rgb(0 0 0 / 0.4);
}

.cabinet-login__brand {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
  text-align: center;
}

.cabinet-login__brand-title {
  margin: 0;
  font-size: 40px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #fff;
  line-height: 1.1;
}

.cabinet-login__brand-subtitle {
  margin: 0;
  font-size: 16px;
  font-weight: 300;
  color: rgb(255 255 255 / 0.65);
}

.cabinet-login__welcome {
  margin: 8px 0 28px;
  font-size: 20px;
  font-weight: 300;
  text-align: center;
  color: #fff;
}

.cabinet-login__notice {
  margin: 0 0 16px;
  padding: 8px 12px;
  border: 1px solid rgb(245 200 66 / 0.3);
  border-radius: 8px;
  background: rgb(245 200 66 / 0.12);
  font-size: 14px;
  text-align: center;
  color: rgb(255 255 255 / 0.85);
}

.cabinet-login__form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cabinet-login__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/*
 * Label из @/components/ui рендерится с базовыми Tailwind-классами
 * (text-sm font-medium text-ink-strong), которые имеют ту же specificity,
 * что и наш scoped class. Чтобы гарантированно получить muted-цвет PAM-стиля
 * — повышаем приоритет через `!important`.
 */
.cabinet-login__label {
  font-size: 13px !important;
  font-weight: 400 !important;
  color: rgb(255 255 255 / 0.65) !important;
}

/*
 * Перебиваем дефолтные стили Input.vue (`bg-surface`, `text-ink-strong`,
 * `border-border`) на полупрозрачно-тёмные — чтобы инпут вписался в
 * стеклянную карточку. !important нужен, потому что Input.vue ставит
 * статические Tailwind-классы на корневой <input>, и без !important
 * наш override не побеждает specificity.
 */
.cabinet-login__input {
  background: rgb(255 255 255 / 0.06) !important;
  border-color: rgb(255 255 255 / 0.18) !important;
  color: #fff !important;
  height: 44px !important;
}

.cabinet-login__input::placeholder {
  color: rgb(255 255 255 / 0.4);
}

.cabinet-login__input:focus,
.cabinet-login__input:focus-visible {
  border-color: var(--c-brand-500) !important;
  background: rgb(255 255 255 / 0.08) !important;
  box-shadow: 0 0 0 3px rgb(245 200 66 / 0.18) !important;
  outline: none !important;
}

.cabinet-login__hint {
  margin: 0;
  font-size: 12px;
  color: rgb(255 255 255 / 0.65);
}

.cabinet-login__error {
  min-height: 1.25rem;
  margin: 0;
  font-size: 13px;
  text-align: left;
  color: var(--c-danger);
}

.cabinet-login__submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 52px;
  margin-top: 8px;
  padding: 0 16px;
  border: 1px solid rgb(255 255 255 / 0.4);
  border-radius: 9px;
  background: transparent;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition:
    box-shadow var(--transition-base),
    border-color var(--transition-base),
    color var(--transition-base);
}

.cabinet-login__submit:hover:not(:disabled) {
  border-color: var(--c-brand-500);
  color: var(--c-brand-500);
  box-shadow: 0 0 12px 0 rgb(245 200 66 / 0.45);
}

.cabinet-login__submit:focus-visible {
  outline: 2px solid var(--c-brand-500);
  outline-offset: 2px;
}

.cabinet-login__submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cabinet-login__submit-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: cabinet-login-spin 0.8s linear infinite;
}

@keyframes cabinet-login-spin {
  to {
    transform: rotate(360deg);
  }
}

.cabinet-login__noscript {
  position: fixed;
  inset-inline: 0;
  bottom: 0;
  padding: 12px 16px;
  background: rgb(245 200 66 / 0.18);
  text-align: center;
  font-size: 14px;
  color: #fff;
}

@media (max-width: 640px) {
  .cabinet-login__card {
    max-width: 100%;
    padding: 28px 20px 24px;
  }
  .cabinet-login__brand-title {
    font-size: 32px;
  }
  .cabinet-login__org {
    top: 16px;
    left: 16px;
  }
  .cabinet-login__org-img {
    height: 36px;
  }
}
</style>

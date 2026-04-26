/**
 * Pinia store: текущий пользователь и сессия Кабинета.
 * См. specs/006-spa-cabinet-ui/data-model.md §3.1.
 *
 * Store подключает обработчик 401 → переводит UI в состояние "session-expired"
 * перед full-page redirect на страницу логина.
 */

import type { LoginOutcome } from '@/api/auth-service'
import type { BootPayload, PermissionFlags } from '@/api/types/domain'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { AUTH_TOKEN_STORAGE_KEY, fetchAuthenticatedUser, loginViaCabinet, logoutFromCabinet } from '@/api/auth-service'
import { readBoot } from '@/api/boot'
import { buildPermissions } from '@/api/composables/use-permissions'
import { onSessionExpired } from '@/api/api-client'
import { disconnectSocket } from '@/api/socket'
import { useNavigationStore } from '@/stores/navigation'
import { sanitizeFrom } from '@/utils/url'

const GUEST = 'Guest'

/**
 * Роли, которые дают доступ хотя бы к одному разделу Кабинета (009-cabinet-custom-login,
 * data-model.md §2.1). Если у пользователя нет ни одной из них и он не админ —
 * router-guard направляет его на `/cabinet/no-modules` (FR-021).
 */
export const CABINET_ROLES = [
  'Order Manager',
  'Shop Supervisor',
  'Executor',
  'Warehouse',
  'Order Corrector',
] as const

export const useAuthStore = defineStore('auth', () => {
  const boot = readBoot()

  const user = ref<string | null>(boot.user && boot.user !== GUEST ? boot.user : null)
  const roles = ref<string[]>([...boot.roles])
  const language = ref<string>(boot.language)
  const csrfToken = ref<string>(boot.csrfToken)
  const siteName = ref<string>(boot.siteName)
  const deskUrl = ref<string>(boot.deskUrl)
  const cabinetVersion = ref<string>(boot.cabinetVersion)
  const sessionExpired = ref(false)
  const loginRedirectFrom = ref<string | null>(null)

  const isAuthenticated = computed(() => !!user.value && !sessionExpired.value)

  const permissions = computed<PermissionFlags>(() =>
    buildPermissions(user.value, roles.value),
  )

  /** Список cabinet-ролей текущего пользователя (пересечение `roles` и `CABINET_ROLES`). */
  const cabinetRoles = computed<string[]>(() =>
    roles.value.filter(role => (CABINET_ROLES as readonly string[]).includes(role)),
  )

  /**
   * `true`, если у пользователя есть хотя бы одна cabinet-роль ИЛИ он админ
   * (Administrator / System Manager). Используется guard'ом для редиректа
   * на `/cabinet/no-modules` (009 FR-021).
   */
  const hasCabinetAccess = computed<boolean>(() => {
    if (permissions.value.isAdmin)
      return true
    return cabinetRoles.value.length > 0
  })

  function applyBoot(payload: BootPayload): void {
    user.value = payload.user && payload.user !== GUEST ? payload.user : null
    roles.value = [...payload.roles]
    language.value = payload.language
    csrfToken.value = payload.csrfToken
    siteName.value = payload.siteName
    deskUrl.value = payload.deskUrl
    cabinetVersion.value = payload.cabinetVersion
    sessionExpired.value = false
  }

  function markSessionExpired(): void {
    sessionExpired.value = true
  }

  function clearSessionExpired(): void {
    sessionExpired.value = false
  }

  function rememberRedirect(path: string | null): void {
    loginRedirectFrom.value = path
  }

  async function logout(): Promise<void> {
    await logoutFromCabinet()
    window.localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
    user.value = null
    roles.value = []
    sessionExpired.value = false
    csrfToken.value = ''
    disconnectSocket()
    // 010 US3 (T034 / NS-G2): новый пользователь не должен унаследовать
    // навигационную историю предыдущего. Очищаем стек ПОСЛЕ logout API,
    // ДО редиректа на /cabinet/login (редирект делает caller — TopBar).
    useNavigationStore().clear()
  }

  /**
   * Выполнить логин через Кабинет-форму (009-cabinet-custom-login).
   *
   * @param usr Логин (email).
   * @param pwd Пароль.
   * @param targetUrl Куда редиректить после успеха (приходит из `?from=` query).
   *                  Прогоняется через `sanitizeFrom`; невалидное → `/cabinet`.
   */
  async function login(
    usr: string,
    pwd: string,
    targetUrl: string | null | undefined,
  ): Promise<LoginOutcome> {
    const outcome = await loginViaCabinet(usr, pwd)
    if (outcome.kind === 'success') {
      window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, outcome.accessToken)
      user.value = outcome.user.login
      roles.value = [...outcome.user.roles]
      sessionExpired.value = false
      const safe = sanitizeFrom(targetUrl) ?? '/cabinet'
      window.location.assign(safe)
    }
    return outcome
  }

  async function bootstrapFromStoredToken(): Promise<boolean> {
    const token = window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
    if (!token)
      return false

    try {
      const session = await fetchAuthenticatedUser(token)
      user.value = session.login
      roles.value = [...session.roles]
      sessionExpired.value = false
      return true
    }
    catch {
      window.localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
      user.value = null
      roles.value = []
      sessionExpired.value = true
      return false
    }
  }

  async function refreshBoot(): Promise<void> {
    const token = window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
    if (!token)
      return
    await bootstrapFromStoredToken()
  }

  // Регистрируем глобальный обработчик 401: состояние сбрасывается до full-page redirect.
  onSessionExpired(() => markSessionExpired())

  return {
    user,
    roles,
    language,
    csrfToken,
    siteName,
    deskUrl,
    cabinetVersion,
    sessionExpired,
    loginRedirectFrom,
    isAuthenticated,
    permissions,
    cabinetRoles,
    hasCabinetAccess,
    applyBoot,
    markSessionExpired,
    clearSessionExpired,
    rememberRedirect,
    login,
    bootstrapFromStoredToken,
    logout,
    refreshBoot,
  }
})

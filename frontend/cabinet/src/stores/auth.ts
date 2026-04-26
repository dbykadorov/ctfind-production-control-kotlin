/**
 * Pinia store: текущий пользователь и сессия Кабинета.
 * См. specs/006-spa-cabinet-ui/data-model.md §3.1.
 *
 * Источник правды — boot-payload, инжектируемый сервером (см. www/cabinet/index.py).
 * Store также подключает обработчик 401 → переводит UI в состояние "session-expired"
 * без сброса draft'ов (FR-011).
 */

import type { LoginOutcome } from '@/api/auth-service'
import type { BootPayload, PermissionFlags } from '@/api/types/domain'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { loginViaCabinet, logoutFromCabinet } from '@/api/auth-service'
import { readBoot } from '@/api/boot'
import { buildPermissions } from '@/api/composables/use-permissions'
import { frappeCall, onSessionExpired } from '@/api/frappe-client'
import { disconnectSocket } from '@/api/socket'
import { useNavigationStore } from '@/stores/navigation'

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
   * В текущем feature-slice реальный success невозможен: сервис возвращает
   * placeholder error, а store не создает сессию и не делает redirect.
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
    void targetUrl
    const outcome = await loginViaCabinet(usr, pwd)
    return outcome
  }

  /**
   * Опциональный refresh boot-payload через `cabinet.boot.get_boot_payload`
   * (см. contracts/http-endpoints.md). Если эндпоинт отсутствует — fallback к
   * `frappe.client.get_logged_user`.
   */
  async function refreshBoot(): Promise<void> {
    try {
      const payload = await frappeCall<BootPayload>(
        'ctfind_production_control.production_control.cabinet.boot.get_boot_payload',
        {},
        { method: 'GET' },
      )
      applyBoot(payload)
    }
    catch {
      const userName = await frappeCall<string>('frappe.auth.get_logged_user', {}, { method: 'GET' })
      if (userName && userName !== GUEST) {
        user.value = userName
        sessionExpired.value = false
      }
    }
  }

  // Регистрируем глобальный обработчик 401 → session-expired (без редиректа)
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
    logout,
    refreshBoot,
  }
})

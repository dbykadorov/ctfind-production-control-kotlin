/**
 * useNavigationStore — внутренний навигационный стек Кабинета (010, US3).
 *
 * Контракт: specs/010-cabinet-layout-rework/contracts/navigation-store.contract.md
 *
 * Хранит линейный стек посещённых маршрутов внутри `/cabinet/*` для работы
 * BackButton'а в TopBar. НЕ персистится между перезагрузками вкладки (R-002).
 *
 * Ключевые гарантии (см. §5 контракта):
 *   - NS-G1: push идемпотентен относительно дубликата вершины
 *   - NS-G2: /cabinet/login никогда не попадает в стек
 *   - NS-G3: пути вне /cabinet/* игнорируются
 *   - NS-G4: stack.length ≤ MAX_STACK_SIZE (50, FIFO trim)
 *   - NS-G5: popPrev — чистая state-функция (не навигирует сам)
 *   - NS-G6: clear() сбрасывает стек до []
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

/**
 * Максимальная глубина стека. Превышение → удаляем старейший элемент (FIFO).
 * Экспортируется как named для unit-тестов (см. tests/unit/stores/navigation.test.ts).
 */
export const MAX_STACK_SIZE = 50

const CABINET_PREFIX = '/cabinet'
const LOGIN_PATH_PREFIX = '/cabinet/login'

function isCabinetPath(fullPath: string): boolean {
  return fullPath === CABINET_PREFIX || fullPath.startsWith(`${CABINET_PREFIX}/`) || fullPath.startsWith(`${CABINET_PREFIX}?`)
}

function isLoginPath(fullPath: string): boolean {
  return fullPath === LOGIN_PATH_PREFIX || fullPath.startsWith(`${LOGIN_PATH_PREFIX}?`) || fullPath.startsWith(`${LOGIN_PATH_PREFIX}/`)
}

export const useNavigationStore = defineStore('navigation', () => {
  const stack = ref<string[]>([])

  const canGoBack = computed(() => stack.value.length >= 2)

  function push(fullPath: string): void {
    if (!isCabinetPath(fullPath))
      return
    if (isLoginPath(fullPath))
      return
    if (stack.value[stack.value.length - 1] === fullPath)
      return // NS-G1
    stack.value.push(fullPath)
    if (stack.value.length > MAX_STACK_SIZE) {
      stack.value.splice(0, stack.value.length - MAX_STACK_SIZE)
    }
  }

  function popPrev(): string | null {
    if (stack.value.length < 2)
      return null
    stack.value.pop()
    return stack.value[stack.value.length - 1] ?? null
  }

  function clear(): void {
    stack.value = []
  }

  return { stack, canGoBack, push, popPrev, clear }
})

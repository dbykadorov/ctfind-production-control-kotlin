/**
 * Unit-тесты useNavigationStore (010-cabinet-layout-rework, T029 / US3).
 *
 * Контракт: specs/010-cabinet-layout-rework/contracts/navigation-store.contract.md §6
 *   - push добавляет / не дублирует / игнорирует non-/cabinet и /cabinet/login
 *   - MAX_STACK_SIZE = 50 (FIFO trim)
 *   - popPrev возвращает null при < 2, иначе удаляет вершину и возвращает следующую
 *   - canGoBack getter
 *   - clear()
 */

import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { MAX_STACK_SIZE, useNavigationStore } from '@/stores/navigation'

describe('useNavigationStore (010 US3)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('push добавляет элемент в стек', () => {
    const nav = useNavigationStore()
    nav.push('/cabinet/dashboard')
    expect(nav.stack).toEqual(['/cabinet/dashboard'])
  })

  it('push не добавляет дубликат предыдущей вершины (NS-G1)', () => {
    const nav = useNavigationStore()
    nav.push('/cabinet/dashboard')
    nav.push('/cabinet/dashboard')
    expect(nav.stack).toEqual(['/cabinet/dashboard'])
  })

  it('push игнорирует пути вне /cabinet/* (NS-G3)', () => {
    const nav = useNavigationStore()
    nav.push('/api/legacy/test')
    nav.push('/desk')
    nav.push('/assets/foo.svg')
    expect(nav.stack).toEqual([])
  })

  it('push игнорирует /cabinet/login (NS-G2)', () => {
    const nav = useNavigationStore()
    nav.push('/cabinet/login')
    nav.push('/cabinet/login?from=%2Fcabinet')
    expect(nav.stack).toEqual([])
  })

  it('push соблюдает MAX_STACK_SIZE = 50 (FIFO trim)', () => {
    const nav = useNavigationStore()
    expect(MAX_STACK_SIZE).toBe(50)
    for (let i = 0; i < 60; i++) {
      nav.push(`/cabinet/orders/${i}`)
    }
    expect(nav.stack.length).toBe(50)
    expect(nav.stack[0]).toBe('/cabinet/orders/10')
    expect(nav.stack[49]).toBe('/cabinet/orders/59')
  })

  it('popPrev возвращает null при стеке < 2', () => {
    const nav = useNavigationStore()
    expect(nav.popPrev()).toBeNull()
    nav.push('/cabinet')
    expect(nav.popPrev()).toBeNull()
  })

  it('popPrev возвращает предыдущий элемент и удаляет вершину', () => {
    const nav = useNavigationStore()
    nav.push('/cabinet')
    nav.push('/cabinet/orders')
    nav.push('/cabinet/orders/123')
    expect(nav.popPrev()).toBe('/cabinet/orders')
    expect(nav.stack).toEqual(['/cabinet', '/cabinet/orders'])
  })

  it('canGoBack === false при стеке размером 0 или 1', () => {
    const nav = useNavigationStore()
    expect(nav.canGoBack).toBe(false)
    nav.push('/cabinet')
    expect(nav.canGoBack).toBe(false)
  })

  it('canGoBack === true при стеке ≥ 2', () => {
    const nav = useNavigationStore()
    nav.push('/cabinet')
    nav.push('/cabinet/orders')
    expect(nav.canGoBack).toBe(true)
  })

  it('clear очищает стек (NS-G6)', () => {
    const nav = useNavigationStore()
    nav.push('/cabinet')
    nav.push('/cabinet/orders')
    nav.clear()
    expect(nav.stack).toEqual([])
    expect(nav.canGoBack).toBe(false)
  })
})

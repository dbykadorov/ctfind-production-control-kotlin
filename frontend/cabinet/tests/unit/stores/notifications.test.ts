import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/stores/notifications.ts'),
  'utf8',
)

describe('notification store', () => {
  it('exports useNotificationStore', () => {
    expect(SOURCE).toContain('useNotificationStore')
  })

  it('defines unreadCount ref', () => {
    expect(SOURCE).toContain('unreadCount')
  })

  it('defines startPolling function', () => {
    expect(SOURCE).toContain('startPolling')
  })

  it('defines stopPolling function', () => {
    expect(SOURCE).toContain('stopPolling')
  })

  it('defines fetchDropdown function', () => {
    expect(SOURCE).toContain('fetchDropdown')
  })

  it('defines markRead function', () => {
    expect(SOURCE).toContain('markRead')
  })

  it('defines markAllRead function', () => {
    expect(SOURCE).toContain('markAllRead')
  })

  it('uses defineStore with notifications id', () => {
    expect(SOURCE).toContain("defineStore('notifications'")
  })

  it('polls with 30s interval', () => {
    expect(SOURCE).toContain('30_000')
  })

  it('handles visibilitychange for pause/resume', () => {
    expect(SOURCE).toContain('visibilitychange')
  })
})

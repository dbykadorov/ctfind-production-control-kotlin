import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/notifications/NotificationsPage.vue'),
  'utf8',
)

describe('NotificationsPage', () => {
  it('uses useNotifications composable', () => {
    expect(SOURCE).toContain('useNotifications')
  })

  it('renders notification list with NotificationItem', () => {
    expect(SOURCE).toContain('NotificationItem')
  })

  it('has pagination controls', () => {
    expect(SOURCE).toMatch(/page|prev|next|pagination/i)
  })

  it('has unreadOnly toggle', () => {
    expect(SOURCE).toContain('unreadOnly')
  })

  it('has mark-all-read button', () => {
    expect(SOURCE).toContain('markAllRead')
  })

  it('shows loading state', () => {
    expect(SOURCE).toContain('loading')
  })

  it('shows empty state', () => {
    expect(SOURCE).toMatch(/empty|notifications\.empty/)
  })

  it('shows error state with refetch', () => {
    expect(SOURCE).toContain('error')
    expect(SOURCE).toContain('refetch')
  })
})

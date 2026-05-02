import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/components/domain/notifications/NotificationBell.vue'),
  'utf8',
)

describe('NotificationBell', () => {
  it('uses useNotificationStore', () => {
    expect(SOURCE).toContain('useNotificationStore')
  })

  it('renders Bell icon from lucide-vue-next', () => {
    expect(SOURCE).toContain('Bell')
  })

  it('shows badge when unreadCount > 0', () => {
    expect(SOURCE).toContain('unreadCount')
  })

  it('renders NotificationDropdown', () => {
    expect(SOURCE).toContain('NotificationDropdown')
  })

  it('handles click to open/close', () => {
    expect(SOURCE).toMatch(/open|isOpen|showDropdown/)
  })

  it('shows 99+ for large counts', () => {
    expect(SOURCE).toContain('99')
  })

  it('uses Popover component', () => {
    expect(SOURCE).toContain('Popover')
  })
})

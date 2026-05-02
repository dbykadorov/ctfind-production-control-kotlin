import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/router/index.ts'),
  'utf8',
)
const ROLE_SOURCE = readFileSync(
  join(process.cwd(), 'src/api/roles.ts'),
  'utf8',
)

describe('router users route', () => {
  it('defines /cabinet/users route', () => {
    expect(SOURCE).toContain("path: 'users'")
    expect(SOURCE).toContain("name: 'users.list'")
  })

  it('loads UsersPage component', () => {
    expect(SOURCE).toContain("import('@/pages/admin/UsersPage.vue')")
  })

  it('protects users route with ADMIN role', () => {
    expect(SOURCE).toContain('roles: ROUTE_ROLE_GROUPS.adminOnly')
    expect(ROLE_SOURCE).toContain("admin: 'ADMIN'")
    expect(ROLE_SOURCE).toContain('adminOnly:')
    expect(SOURCE).toContain("title: 'meta.title.users.list'")
  })

  it('keeps forbidden route available for unauthorized redirects', () => {
    expect(SOURCE).toContain("name: 'forbidden'")
    expect(SOURCE).toContain("path: '403'")
  })
})

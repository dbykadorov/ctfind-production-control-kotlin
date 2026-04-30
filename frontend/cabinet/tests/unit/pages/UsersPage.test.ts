import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const SOURCE = readFileSync(
  join(process.cwd(), 'src/pages/admin/UsersPage.vue'),
  'utf8',
)

describe('UsersPage', () => {
  it('uses users list composable', () => {
    expect(SOURCE).toContain('useUsersList')
    expect(SOURCE).toContain('refetch')
  })

  it('fetches role catalog for create form', () => {
    expect(SOURCE).toContain('fetchRoleCatalog')
    expect(SOURCE).toContain('roles')
  })

  it('creates user through API composable', () => {
    expect(SOURCE).toContain('createUser')
    expect(SOURCE).toContain('submitCreate')
  })

  it('updates user through API composable', () => {
    expect(SOURCE).toContain('updateUser')
    expect(SOURCE).toContain('submitEdit')
    expect(SOURCE).toContain('parseUpdateUserError')
  })

  it('renders required create fields: login, displayName, initialPassword, roles', () => {
    expect(SOURCE).toContain('users.fields.login')
    expect(SOURCE).toContain('users.fields.displayName')
    expect(SOURCE).toContain('users.fields.initialPassword')
    expect(SOURCE).toContain('users.fields.roles')
  })

  it('shows user roles in list rows', () => {
    expect(SOURCE).toContain('user.roles')
    expect(SOURCE).toContain('role.name')
  })

  it('handles duplicate/validation/forbidden errors', () => {
    expect(SOURCE).toContain('users.messages.duplicate')
    expect(SOURCE).toContain('users.messages.validation')
    expect(SOURCE).toContain('users.messages.forbidden')
  })

  it('renders edit action and edit modal messages', () => {
    expect(SOURCE).toContain('users.actions.edit')
    expect(SOURCE).toContain('users.edit.title')
    expect(SOURCE).toContain('users.edit.subtitle')
    expect(SOURCE).toContain('users.edit.success')
    expect(SOURCE).toContain('users.edit.errors.userNotFound')
    expect(SOURCE).toContain('users.edit.errors.lastAdminGuard')
  })
})

import type { UserSummaryResponse } from '@/api/types/audit-log'
import { fetchUsers as fetchAdminUsers } from '@/api/composables/use-users'

export async function fetchUsers(
  search?: string,
  limit?: number,
): Promise<UserSummaryResponse[]> {
  const users = await fetchAdminUsers(search, limit)
  return users.map(user => ({
    id: user.id,
    login: user.login,
    displayName: user.displayName,
    roles: user.roles,
  }))
}

import type { UserSummaryResponse } from '@/api/types/audit-log'
import { httpClient } from '@/api/api-client'

export async function fetchUsers(
  search?: string,
  limit?: number,
): Promise<UserSummaryResponse[]> {
  const params: Record<string, string | number> = {}
  if (search?.trim())
    params.search = search.trim()
  if (limit !== undefined)
    params.limit = limit
  const response = await httpClient.get<UserSummaryResponse[]>('/api/users', { params })
  return response.data
}

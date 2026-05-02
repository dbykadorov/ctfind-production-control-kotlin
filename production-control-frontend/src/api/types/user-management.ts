export interface RoleSummaryResponse {
  code: string
  name: string
}

export interface AdminUserSummaryResponse {
  id: string
  login: string
  displayName: string
  roles: RoleSummaryResponse[]
}

export interface CreateUserRequest {
  login: string
  displayName: string
  initialPassword: string
  roleCodes: string[]
}

export interface CreateUserFormState {
  login: string
  displayName: string
  initialPassword: string
  roleCodes: string[]
}

export interface UpdateUserRequest {
  displayName: string
  roleCodes: string[]
}

export interface UpdateUserFormState {
  id: string
  login: string
  displayName: string
  roleCodes: string[]
}

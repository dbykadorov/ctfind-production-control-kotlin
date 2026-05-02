package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import org.springframework.stereotype.Service

sealed interface UserQueryResult {
    data class Success(val users: List<UserSummary>) : UserQueryResult
    data object Forbidden : UserQueryResult
}

@Service
open class UserQueryUseCase(
    private val port: UserQueryPort,
) {
    open fun search(search: String?, limit: Int, roleCodes: Set<String>): UserQueryResult {
        if (ADMIN_ROLE_CODE !in roleCodes) return UserQueryResult.Forbidden
        val cappedLimit = limit.coerceIn(1, 100)
        return UserQueryResult.Success(port.searchUsers(search, cappedLimit))
    }
}

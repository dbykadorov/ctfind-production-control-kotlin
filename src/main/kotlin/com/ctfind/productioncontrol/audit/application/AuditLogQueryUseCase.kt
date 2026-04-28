package com.ctfind.productioncontrol.audit.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import org.springframework.stereotype.Service

sealed interface AuditLogQueryResult {
    data class Success(val page: AuditLogPageResult) : AuditLogQueryResult
    data object Forbidden : AuditLogQueryResult
}

@Service
class AuditLogQueryUseCase(
    private val auditLog: AuditLogQueryPort,
) {
    fun list(query: AuditLogQuery, roleCodes: Set<String>): AuditLogQueryResult {
        if (ADMIN_ROLE_CODE !in roleCodes) return AuditLogQueryResult.Forbidden
        return AuditLogQueryResult.Success(auditLog.search(query))
    }
}

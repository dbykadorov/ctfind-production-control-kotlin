package com.ctfind.productioncontrol.audit.application

interface AuditLogQueryPort {
    fun search(query: AuditLogQuery): AuditLogPageResult
}

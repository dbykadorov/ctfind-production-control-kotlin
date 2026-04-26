package com.ctfind.productioncontrol.auth.application

import org.springframework.stereotype.Service

@Service
class LogoutUseCase(
	private val audit: AuthenticationAuditService,
) {
	fun logout(login: String, requestIp: String?, userAgent: String?) {
		audit.logout(
			login = login,
			userId = null,
			requestIp = requestIp,
			userAgent = userAgent,
		)
	}
}

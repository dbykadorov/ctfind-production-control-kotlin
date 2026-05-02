package com.ctfind.productioncontrol.auth.adapter.persistence

import com.ctfind.productioncontrol.auth.application.EnsureSuperadminResult
import com.ctfind.productioncontrol.auth.application.EnsureSuperadminUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class SuperadminSeedRunner(
	private val useCase: EnsureSuperadminUseCase,
	@Value("\${ctfind.auth.superadmin.login:}")
	private val login: String,
	@Value("\${ctfind.auth.superadmin.display-name:}")
	private val displayName: String,
	@Value("\${ctfind.auth.superadmin.password:}")
	private val secret: String,
) : ApplicationRunner {

	override fun run(args: ApplicationArguments) {
		when (val result = useCase.ensureConfiguredSuperadmin(login = login, displayName = displayName, secret = secret)) {
			is EnsureSuperadminResult.Seeded -> Unit
			is EnsureSuperadminResult.SkippedExistingAdmin -> Unit
			is EnsureSuperadminResult.FailedMissingCredentials ->
				throw IllegalStateException(
					"${result.message}. Set APP_SUPERADMIN_LOGIN, APP_SUPERADMIN_DISPLAY_NAME and APP_SUPERADMIN_PASSWORD.",
				)
		}
	}
}

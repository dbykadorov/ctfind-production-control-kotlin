package com.ctfind.productioncontrol.auth.adapter.persistence

import com.ctfind.productioncontrol.auth.application.LocalAdminSeedUseCase
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local")
class LocalAdminSeedRunner(
	private val seedUseCase: LocalAdminSeedUseCase,
) : ApplicationRunner {

	override fun run(args: ApplicationArguments) {
		seedUseCase.seedLocalAdmin()
	}
}

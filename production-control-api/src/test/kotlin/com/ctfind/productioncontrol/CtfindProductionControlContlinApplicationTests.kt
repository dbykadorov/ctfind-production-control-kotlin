package com.ctfind.productioncontrol

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.SpringBootApplication
import kotlin.test.assertNotNull

class CtfindProductionControlContlinApplicationTests {

	@Test
	fun `application class is a Spring Boot application`() {
		assertNotNull(CtfindProductionControlContlinApplication::class.annotations.filterIsInstance<SpringBootApplication>().singleOrNull())
	}

}

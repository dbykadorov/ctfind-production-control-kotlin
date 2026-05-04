package com.ctfind.productioncontrol.integration

import com.ctfind.productioncontrol.CtfindProductionControlContlinApplication
import com.ctfind.productioncontrol.auth.application.EnsureSuperadminUseCase
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(classes = [CtfindProductionControlContlinApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class IntegrationTestSupport {

	@Autowired
	lateinit var mockMvc: MockMvc

	val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

	@Autowired
	lateinit var jdbc: JdbcTemplate

	@Autowired
	lateinit var ensureSuperadmin: EnsureSuperadminUseCase

	@BeforeEach
	fun resetIntegrationDatabase() {
		jdbc.execute(
			"""
			DO ${'$'}${'$'}
			DECLARE
				table_record RECORD;
			BEGIN
				FOR table_record IN
					SELECT tablename
					FROM pg_tables
					WHERE schemaname = 'public'
						AND tablename NOT IN ('flyway_schema_history', 'app_role')
				LOOP
					EXECUTE 'TRUNCATE TABLE public.' || quote_ident(table_record.tablename) || ' RESTART IDENTITY CASCADE';
				END LOOP;
			END
			${'$'}${'$'};
			""".trimIndent(),
		)
		ensureSuperadmin.ensureConfiguredSuperadmin(
			login = SUPERADMIN_LOGIN,
			displayName = SUPERADMIN_DISPLAY_NAME,
			secret = SUPERADMIN_PASSWORD,
		)
	}

	fun json(result: MvcResult): JsonNode =
		objectMapper.readTree(result.response.contentAsString)

	fun adminToken(): String =
		login(SUPERADMIN_LOGIN, SUPERADMIN_PASSWORD)

	fun login(login: String, password: String): String {
		val result = postJson(
			path = "/api/auth/login",
			body = mapOf("login" to login, "password" to password),
		)
		return json(result)["accessToken"].asText()
	}

	companion object {
		const val SUPERADMIN_LOGIN = "admin.integration"
		const val SUPERADMIN_DISPLAY_NAME = "Integration Administrator"
		const val SUPERADMIN_PASSWORD = "admin-integration-password"

		@Container
		@JvmStatic
		val postgres: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
			withDatabaseName("ctfind_integration")
			withUsername("ctfind")
			withPassword("ctfind")
		}

		@DynamicPropertySource
		@JvmStatic
		fun registerPostgresProperties(registry: DynamicPropertyRegistry) {
			registry.add("spring.datasource.url", postgres::getJdbcUrl)
			registry.add("spring.datasource.username", postgres::getUsername)
			registry.add("spring.datasource.password", postgres::getPassword)
			registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName)
		}
	}
}

package com.ctfind.productioncontrol.auth.adapter.web

import com.ctfind.productioncontrol.auth.application.UserQueryPort
import com.ctfind.productioncontrol.auth.application.UserQueryResult
import com.ctfind.productioncontrol.auth.application.UserQueryUseCase
import com.ctfind.productioncontrol.auth.application.UserSummary
import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class UserControllerTests {

	private val actorId = UUID.fromString("30000000-0000-0000-0000-000000000003")

	private val sampleUser1 = UserSummary(
		id = UUID.fromString("10000000-0000-0000-0000-000000000001"),
		login = "ivanov",
		displayName = "Иванов Иван",
	)

	private val sampleUser2 = UserSummary(
		id = UUID.fromString("10000000-0000-0000-0000-000000000002"),
		login = "petrov",
		displayName = "Петров Пётр",
	)

	@Test
	fun `GET users returns 200 with user list for ADMIN`() {
		val controller = UserController(
			useCase = stubUserQueryUseCase { _, _, _ ->
				UserQueryResult.Success(listOf(sampleUser1, sampleUser2))
			},
		)

		val response = controller.list(
			search = null,
			limit = 50,
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertEquals(HttpStatus.OK, response.statusCode)
		val body = assertIs<List<UserSummaryResponse>>(response.body)
		assertEquals(2, body.size)
		assertEquals(sampleUser1.id, body[0].id)
		assertEquals("ivanov", body[0].login)
		assertEquals("Иванов Иван", body[0].displayName)
		assertEquals(sampleUser2.id, body[1].id)
		assertEquals("petrov", body[1].login)
		assertEquals("Петров Пётр", body[1].displayName)
	}

	@Test
	fun `GET users returns 403 for non-ADMIN`() {
		val controller = UserController(
			useCase = stubUserQueryUseCase { _, _, _ ->
				UserQueryResult.Forbidden
			},
		)

		val response = controller.list(
			search = null,
			limit = 50,
			jwt = jwtFor(actorId, setOf("ORDER_MANAGER")),
		)

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
		val err = assertIs<AuthErrorResponse>(response.body)
		assertEquals("forbidden", err.code)
	}

	@Test
	fun `search param forwarded correctly`() {
		var capturedSearch: String? = "NOT_CALLED"

		val controller = UserController(
			useCase = stubUserQueryUseCase { search, _, _ ->
				capturedSearch = search
				UserQueryResult.Success(listOf(sampleUser1))
			},
		)

		controller.list(
			search = "иван",
			limit = 50,
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertEquals("иван", capturedSearch)
	}

	@Test
	fun `limit param defaults to 50 and is capped at 100`() {
		var capturedLimit: Int? = null

		val controller = UserController(
			useCase = stubUserQueryUseCase { _, limit, _ ->
				capturedLimit = limit
				UserQueryResult.Success(emptyList())
			},
		)

		// Default limit
		controller.list(
			search = null,
			limit = 50,
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)
		assertEquals(50, capturedLimit)

		// Explicit limit
		controller.list(
			search = null,
			limit = 25,
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)
		assertEquals(25, capturedLimit)

		// Capped at 100
		controller.list(
			search = null,
			limit = 200,
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)
		assertEquals(100, capturedLimit)
	}

	@Test
	fun `response shape contains id, login, displayName`() {
		val controller = UserController(
			useCase = stubUserQueryUseCase { _, _, _ ->
				UserQueryResult.Success(listOf(sampleUser1))
			},
		)

		val response = controller.list(
			search = null,
			limit = 50,
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertEquals(HttpStatus.OK, response.statusCode)
		val body = assertIs<List<UserSummaryResponse>>(response.body)
		assertEquals(1, body.size)
		val user = body.first()
		assertNotNull(user.id)
		assertNotNull(user.login)
		assertNotNull(user.displayName)
		assertEquals(sampleUser1.id, user.id)
		assertEquals(sampleUser1.login, user.login)
		assertEquals(sampleUser1.displayName, user.displayName)
	}

	// ---- Test support ----

	private fun jwtFor(actorId: UUID, roles: Set<String>): Jwt =
		Jwt.withTokenValue("tok")
			.header("alg", "none")
			.subject("user1")
			.claim("userId", actorId.toString())
			.claim("displayName", "Tester")
			.claim("roles", roles.toList())
			.build()

	private fun unusedQueryPort(): UserQueryPort = object : UserQueryPort {
		override fun searchUsers(search: String?, limit: Int): List<UserSummary> = emptyList()
	}

	private fun stubUserQueryUseCase(
		exec: (String?, Int, Set<String>) -> UserQueryResult,
	): UserQueryUseCase = object : UserQueryUseCase(
		port = unusedQueryPort(),
	) {
		override fun search(search: String?, limit: Int, roleCodes: Set<String>): UserQueryResult =
			exec(search, limit, roleCodes)
	}
}

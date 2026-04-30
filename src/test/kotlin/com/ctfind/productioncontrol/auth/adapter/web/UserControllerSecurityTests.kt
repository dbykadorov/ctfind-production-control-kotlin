package com.ctfind.productioncontrol.auth.adapter.web

import com.ctfind.productioncontrol.auth.application.AuthenticationAuditPort
import com.ctfind.productioncontrol.auth.application.CreateUserCommand
import com.ctfind.productioncontrol.auth.application.CreateUserResult
import com.ctfind.productioncontrol.auth.application.CreateUserUseCase
import com.ctfind.productioncontrol.auth.application.RoleCatalogPort
import com.ctfind.productioncontrol.auth.application.RoleCatalogResult
import com.ctfind.productioncontrol.auth.application.RoleCatalogUseCase
import com.ctfind.productioncontrol.auth.application.RolePort
import com.ctfind.productioncontrol.auth.application.RoleSummary
import com.ctfind.productioncontrol.auth.application.UpdateUserCommand
import com.ctfind.productioncontrol.auth.application.UpdateUserResult
import com.ctfind.productioncontrol.auth.application.UpdateUserUseCase
import com.ctfind.productioncontrol.auth.application.UserAccountPort
import com.ctfind.productioncontrol.auth.application.UserQueryPort
import com.ctfind.productioncontrol.auth.application.UserQueryResult
import com.ctfind.productioncontrol.auth.application.UserQueryUseCase
import com.ctfind.productioncontrol.auth.application.UserSummary
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.Role
import com.ctfind.productioncontrol.auth.domain.UserAccount
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Clock
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UserControllerSecurityTests {

	private val actorId = UUID.fromString("30000000-0000-0000-0000-000000000003")

	@Test
	fun `GET users returns forbidden payload for non-admin roles`() {
		val controller = controller(
			query = UserQueryResult.Forbidden,
			create = CreateUserResult.Forbidden,
			roles = RoleCatalogResult.Forbidden,
			update = UpdateUserResult.Forbidden,
		)

		val response = controller.list(
			search = null,
			limit = 50,
			jwt = jwtFor(actorId, setOf("WAREHOUSE")),
		)

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
		assertEquals("forbidden", assertIs<AuthErrorResponse>(response.body).code)
	}

	@Test
	fun `POST users returns forbidden payload for non-admin roles`() {
		val controller = controller(
			query = UserQueryResult.Forbidden,
			create = CreateUserResult.Forbidden,
			roles = RoleCatalogResult.Forbidden,
			update = UpdateUserResult.Forbidden,
		)

		val response = controller.create(
			request = CreateUserRequest(
				login = "warehouse.demo",
				displayName = "Warehouse Demo",
				initialPassword = "demo",
				roleCodes = setOf("WAREHOUSE"),
			),
			jwt = jwtFor(actorId, setOf("WAREHOUSE")),
		)

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
		assertEquals("forbidden", assertIs<AuthErrorResponse>(response.body).code)
	}

	@Test
	fun `GET users roles returns forbidden payload for non-admin roles`() {
		val controller = controller(
			query = UserQueryResult.Forbidden,
			create = CreateUserResult.Forbidden,
			roles = RoleCatalogResult.Forbidden,
			update = UpdateUserResult.Forbidden,
		)

		val response = controller.listRoles(jwtFor(actorId, setOf("PRODUCTION_EXECUTOR")))

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
		assertEquals("forbidden", assertIs<AuthErrorResponse>(response.body).code)
	}

	@Test
	fun `PUT users returns forbidden payload for non-admin roles`() {
		val controller = controller(
			query = UserQueryResult.Forbidden,
			create = CreateUserResult.Forbidden,
			roles = RoleCatalogResult.Forbidden,
			update = UpdateUserResult.Forbidden,
		)

		val response = controller.update(
			userId = UUID.fromString("10000000-0000-0000-0000-000000000001"),
			request = UpdateUserRequest(
				displayName = "Updated",
				roleCodes = setOf("WAREHOUSE"),
			),
			jwt = jwtFor(actorId, setOf("PRODUCTION_EXECUTOR")),
		)

		assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
		assertEquals("forbidden", assertIs<AuthErrorResponse>(response.body).code)
	}

	private fun controller(
		query: UserQueryResult,
		create: CreateUserResult,
		roles: RoleCatalogResult,
		update: UpdateUserResult,
	): UserController = UserController(
		queryUseCase = object : UserQueryUseCase(
			port = object : UserQueryPort {
				override fun searchUsers(search: String?, limit: Int): List<UserSummary> = emptyList()
			},
		) {
			override fun search(search: String?, limit: Int, roleCodes: Set<String>): UserQueryResult = query
		},
		createUserUseCase = object : CreateUserUseCase(
			users = dummyUsers(),
			roles = dummyRoles(),
			audit = dummyAudit(),
			passwordEncoder = BCryptPasswordEncoder(),
			clock = Clock.systemUTC(),
		) {
			override fun create(command: CreateUserCommand): CreateUserResult = create
		},
		roleCatalogUseCase = object : RoleCatalogUseCase(
			catalog = dummyRoleCatalog(),
		) {
			override fun list(roleCodes: Set<String>): RoleCatalogResult = roles
		},
		updateUserUseCase = object : UpdateUserUseCase(
			users = dummyUsers(),
			roles = dummyRoles(),
			audit = dummyAudit(),
			clock = Clock.systemUTC(),
		) {
			override fun update(command: UpdateUserCommand): UpdateUserResult = update
		},
	)

	private fun dummyUsers(): UserAccountPort = object : UserAccountPort {
		override fun findByLogin(login: String): UserAccount? = null
		override fun save(user: UserAccount): UserAccount = user
		override fun existsEnabledWithRole(roleCode: String): Boolean = false
	}

	private fun dummyRoles(): RolePort = object : RolePort {
		override fun findByCode(code: String): Role? = null
		override fun save(role: Role): Role = role
		override fun findAllByCodes(codes: Set<String>): List<Role> = emptyList()
	}

	private fun dummyAudit(): AuthenticationAuditPort = object : AuthenticationAuditPort {
		override fun record(event: AuthenticationAuditEvent): AuthenticationAuditEvent = event
	}

	private fun dummyRoleCatalog(): RoleCatalogPort = object : RoleCatalogPort {
		override fun listRoles(codes: Set<String>): List<RoleSummary> = emptyList()
	}

	private fun jwtFor(actorId: UUID, roles: Set<String>): Jwt =
		Jwt.withTokenValue("tok")
			.header("alg", "none")
			.subject("user1")
			.claim("userId", actorId.toString())
			.claim("displayName", "Tester")
			.claim("roles", roles.toList())
			.build()
}

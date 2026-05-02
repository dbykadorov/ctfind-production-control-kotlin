package com.ctfind.productioncontrol.auth.adapter.web

import com.ctfind.productioncontrol.auth.application.AuthenticationAuditPort
import com.ctfind.productioncontrol.auth.application.CreateUserCommand
import com.ctfind.productioncontrol.auth.application.CreateUserResult
import com.ctfind.productioncontrol.auth.application.CreateUserUseCase
import com.ctfind.productioncontrol.auth.application.RoleCatalogPort
import com.ctfind.productioncontrol.auth.application.RoleCatalogResult
import com.ctfind.productioncontrol.auth.application.RoleCatalogUseCase
import com.ctfind.productioncontrol.auth.application.RolePort
import com.ctfind.productioncontrol.auth.application.UserAccountPort
import com.ctfind.productioncontrol.auth.application.RoleSummary
import com.ctfind.productioncontrol.auth.application.UpdateUserCommand
import com.ctfind.productioncontrol.auth.application.UpdateUserResult
import com.ctfind.productioncontrol.auth.application.UpdateUserUseCase
import com.ctfind.productioncontrol.auth.application.UserQueryPort
import com.ctfind.productioncontrol.auth.application.UserQueryResult
import com.ctfind.productioncontrol.auth.application.UserQueryUseCase
import com.ctfind.productioncontrol.auth.application.UserSummary
import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.Role
import com.ctfind.productioncontrol.auth.domain.UserAccount
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Clock
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
		roles = listOf(RoleSummary(code = "ORDER_MANAGER", name = "Order Manager")),
	)

	private val sampleUser2 = UserSummary(
		id = UUID.fromString("10000000-0000-0000-0000-000000000002"),
		login = "petrov",
		displayName = "Петров Пётр",
		roles = listOf(RoleSummary(code = "WAREHOUSE", name = "Warehouse")),
	)

	@Test
	fun `GET users returns 200 with user list for ADMIN`() {
		val controller = UserController(
			queryUseCase = stubUserQueryUseCase { _, _, _ ->
				UserQueryResult.Success(listOf(sampleUser1, sampleUser2))
			},
			createUserUseCase = stubCreateUserUseCase { _ -> CreateUserResult.Forbidden },
			roleCatalogUseCase = stubRoleCatalogUseCase { RoleCatalogResult.Forbidden },
			updateUserUseCase = stubUpdateUserUseCase { UpdateUserResult.Forbidden },
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
		assertEquals("ORDER_MANAGER", body[0].roles.first().code)
		assertEquals(sampleUser2.id, body[1].id)
		assertEquals("petrov", body[1].login)
		assertEquals("Петров Пётр", body[1].displayName)
	}

	@Test
	fun `GET users returns 403 for non-ADMIN`() {
		val controller = UserController(
			queryUseCase = stubUserQueryUseCase { _, _, _ ->
				UserQueryResult.Forbidden
			},
			createUserUseCase = stubCreateUserUseCase { _ -> CreateUserResult.Forbidden },
			roleCatalogUseCase = stubRoleCatalogUseCase { RoleCatalogResult.Forbidden },
			updateUserUseCase = stubUpdateUserUseCase { UpdateUserResult.Forbidden },
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
			queryUseCase = stubUserQueryUseCase { search, _, _ ->
				capturedSearch = search
				UserQueryResult.Success(listOf(sampleUser1))
			},
			createUserUseCase = stubCreateUserUseCase { _ -> CreateUserResult.Forbidden },
			roleCatalogUseCase = stubRoleCatalogUseCase { RoleCatalogResult.Forbidden },
			updateUserUseCase = stubUpdateUserUseCase { UpdateUserResult.Forbidden },
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
			queryUseCase = stubUserQueryUseCase { _, limit, _ ->
				capturedLimit = limit
				UserQueryResult.Success(emptyList())
			},
			createUserUseCase = stubCreateUserUseCase { _ -> CreateUserResult.Forbidden },
			roleCatalogUseCase = stubRoleCatalogUseCase { RoleCatalogResult.Forbidden },
			updateUserUseCase = stubUpdateUserUseCase { UpdateUserResult.Forbidden },
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
			queryUseCase = stubUserQueryUseCase { _, _, _ ->
				UserQueryResult.Success(listOf(sampleUser1))
			},
			createUserUseCase = stubCreateUserUseCase { _ -> CreateUserResult.Forbidden },
			roleCatalogUseCase = stubRoleCatalogUseCase { RoleCatalogResult.Forbidden },
			updateUserUseCase = stubUpdateUserUseCase { UpdateUserResult.Forbidden },
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
		assertEquals(sampleUser1.roles.first().code, user.roles.first().code)
	}

	@Test
	fun `POST users returns 201 when create succeeds`() {
		var capturedCommand: CreateUserCommand? = null
		val created = UserSummary(
			id = UUID.fromString("10000000-0000-0000-0000-000000000010"),
			login = "warehouse.demo",
			displayName = "Warehouse Demo",
			roles = listOf(RoleSummary(code = "WAREHOUSE", name = "Warehouse")),
		)
		val controller = UserController(
			queryUseCase = stubUserQueryUseCase { _, _, _ -> UserQueryResult.Success(emptyList()) },
			createUserUseCase = stubCreateUserUseCase { cmd ->
				capturedCommand = cmd
				CreateUserResult.Success(created)
			},
			roleCatalogUseCase = stubRoleCatalogUseCase { RoleCatalogResult.Forbidden },
			updateUserUseCase = stubUpdateUserUseCase { UpdateUserResult.Forbidden },
		)

		val response = controller.create(
			request = CreateUserRequest(
				login = "warehouse.demo",
				displayName = "Warehouse Demo",
				initialPassword = "demo",
				roleCodes = setOf("WAREHOUSE"),
			),
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertEquals(HttpStatus.CREATED, response.statusCode)
		val body = assertIs<UserSummaryResponse>(response.body)
		assertEquals("warehouse.demo", body.login)
		assertEquals("WAREHOUSE", body.roles.first().code)
		assertEquals("warehouse.demo", capturedCommand?.login)
	}

	@Test
	fun `POST users returns 409 on duplicate login`() {
		val controller = UserController(
			queryUseCase = stubUserQueryUseCase { _, _, _ -> UserQueryResult.Success(emptyList()) },
			createUserUseCase = stubCreateUserUseCase { _ -> CreateUserResult.DuplicateLogin("warehouse.demo") },
			roleCatalogUseCase = stubRoleCatalogUseCase { RoleCatalogResult.Forbidden },
			updateUserUseCase = stubUpdateUserUseCase { UpdateUserResult.Forbidden },
		)

		val response = controller.create(
			request = CreateUserRequest(
				login = "warehouse.demo",
				displayName = "Warehouse Demo",
				initialPassword = "demo",
				roleCodes = setOf("WAREHOUSE"),
			),
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertEquals(HttpStatus.CONFLICT, response.statusCode)
		val body = assertIs<AuthErrorResponse>(response.body)
		assertEquals("duplicate_login", body.code)
	}

	@Test
	fun `GET users roles returns role catalog for ADMIN`() {
		val controller = UserController(
			queryUseCase = stubUserQueryUseCase { _, _, _ -> UserQueryResult.Success(emptyList()) },
			createUserUseCase = stubCreateUserUseCase { _ -> CreateUserResult.Forbidden },
			roleCatalogUseCase = stubRoleCatalogUseCase {
				RoleCatalogResult.Success(
					listOf(
						RoleSummary(code = "ADMIN", name = "Administrator"),
						RoleSummary(code = "WAREHOUSE", name = "Warehouse"),
					),
				)
			},
			updateUserUseCase = stubUpdateUserUseCase { UpdateUserResult.Forbidden },
		)

		val response = controller.listRoles(jwtFor(actorId, setOf(ADMIN_ROLE_CODE)))

		assertEquals(HttpStatus.OK, response.statusCode)
		val body = assertIs<List<RoleSummaryResponse>>(response.body)
		assertEquals(2, body.size)
		assertEquals("ADMIN", body[0].code)
		assertEquals("WAREHOUSE", body[1].code)
	}

	@Test
	fun `PUT users returns 200 when update succeeds`() {
		var capturedCommand: UpdateUserCommand? = null
		val updated = UserSummary(
			id = sampleUser1.id,
			login = sampleUser1.login,
			displayName = "Иванов Иван Обновлён",
			roles = listOf(RoleSummary(code = "WAREHOUSE", name = "Warehouse")),
		)
		val controller = UserController(
			queryUseCase = stubUserQueryUseCase { _, _, _ -> UserQueryResult.Success(emptyList()) },
			createUserUseCase = stubCreateUserUseCase { _ -> CreateUserResult.Forbidden },
			roleCatalogUseCase = stubRoleCatalogUseCase { RoleCatalogResult.Forbidden },
			updateUserUseCase = stubUpdateUserUseCase { cmd ->
				capturedCommand = cmd
				UpdateUserResult.Success(updated)
			},
		)

		val response = controller.update(
			userId = sampleUser1.id,
			request = UpdateUserRequest(
				displayName = "Иванов Иван Обновлён",
				roleCodes = setOf("WAREHOUSE"),
			),
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertEquals(HttpStatus.OK, response.statusCode)
		val body = assertIs<UserSummaryResponse>(response.body)
		assertEquals("Иванов Иван Обновлён", body.displayName)
		assertEquals("WAREHOUSE", body.roles.single().code)
		assertEquals(sampleUser1.id, capturedCommand?.userId)
		assertEquals("Иванов Иван Обновлён", capturedCommand?.displayName)
	}

	@Test
	fun `PUT users returns 404 when target user is missing`() {
		val controller = UserController(
			queryUseCase = stubUserQueryUseCase { _, _, _ -> UserQueryResult.Success(emptyList()) },
			createUserUseCase = stubCreateUserUseCase { _ -> CreateUserResult.Forbidden },
			roleCatalogUseCase = stubRoleCatalogUseCase { RoleCatalogResult.Forbidden },
			updateUserUseCase = stubUpdateUserUseCase { UpdateUserResult.NotFound },
		)

		val response = controller.update(
			userId = sampleUser1.id,
			request = UpdateUserRequest(
				displayName = "Updated",
				roleCodes = setOf("WAREHOUSE"),
			),
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)

		assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
		assertEquals("user_not_found", assertIs<AuthErrorResponse>(response.body).code)
	}

	@Test
	fun `PUT users maps validation invalid_roles and last_admin_guard errors`() {
		val roleErrorController = UserController(
			queryUseCase = stubUserQueryUseCase { _, _, _ -> UserQueryResult.Success(emptyList()) },
			createUserUseCase = stubCreateUserUseCase { _ -> CreateUserResult.Forbidden },
			roleCatalogUseCase = stubRoleCatalogUseCase { RoleCatalogResult.Forbidden },
			updateUserUseCase = stubUpdateUserUseCase { UpdateUserResult.InvalidRoles(setOf("UNKNOWN")) },
		)

		val invalidRolesResponse = roleErrorController.update(
			userId = sampleUser1.id,
			request = UpdateUserRequest(
				displayName = "Updated",
				roleCodes = setOf("UNKNOWN"),
			),
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)
		assertEquals(HttpStatus.BAD_REQUEST, invalidRolesResponse.statusCode)
		assertEquals("invalid_roles", assertIs<AuthErrorResponse>(invalidRolesResponse.body).code)

		val guardController = UserController(
			queryUseCase = stubUserQueryUseCase { _, _, _ -> UserQueryResult.Success(emptyList()) },
			createUserUseCase = stubCreateUserUseCase { _ -> CreateUserResult.Forbidden },
			roleCatalogUseCase = stubRoleCatalogUseCase { RoleCatalogResult.Forbidden },
			updateUserUseCase = stubUpdateUserUseCase { UpdateUserResult.LastAdminRoleRemovalForbidden },
		)

		val guardResponse = guardController.update(
			userId = sampleUser1.id,
			request = UpdateUserRequest(
				displayName = "Updated",
				roleCodes = setOf("WAREHOUSE"),
			),
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)
		assertEquals(HttpStatus.CONFLICT, guardResponse.statusCode)
		assertEquals("last_admin_role_removal_forbidden", assertIs<AuthErrorResponse>(guardResponse.body).code)

		val validationController = UserController(
			queryUseCase = stubUserQueryUseCase { _, _, _ -> UserQueryResult.Success(emptyList()) },
			createUserUseCase = stubCreateUserUseCase { _ -> CreateUserResult.Forbidden },
			roleCatalogUseCase = stubRoleCatalogUseCase { RoleCatalogResult.Forbidden },
			updateUserUseCase = stubUpdateUserUseCase {
				UpdateUserResult.ValidationError("displayName is required", "displayName")
			},
		)
		val validationResponse = validationController.update(
			userId = sampleUser1.id,
			request = UpdateUserRequest(
				displayName = "",
				roleCodes = setOf("WAREHOUSE"),
			),
			jwt = jwtFor(actorId, setOf(ADMIN_ROLE_CODE)),
		)
		assertEquals(HttpStatus.BAD_REQUEST, validationResponse.statusCode)
		assertEquals("validation_error", assertIs<AuthErrorResponse>(validationResponse.body).code)
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

	private fun stubCreateUserUseCase(
		exec: (CreateUserCommand) -> CreateUserResult,
	): CreateUserUseCase = object : CreateUserUseCase(
		users = dummyUsers(),
		roles = dummyRoles(),
		audit = dummyAudit(),
		passwordEncoder = BCryptPasswordEncoder(),
		clock = Clock.systemUTC(),
	) {
		override fun create(command: CreateUserCommand): CreateUserResult = exec(command)
	}

	private fun stubRoleCatalogUseCase(
		exec: () -> RoleCatalogResult,
	): RoleCatalogUseCase = object : RoleCatalogUseCase(
		catalog = dummyRoleCatalog(),
	) {
		override fun list(roleCodes: Set<String>): RoleCatalogResult = exec()
	}

	private fun stubUpdateUserUseCase(
		exec: (UpdateUserCommand) -> UpdateUserResult,
	): UpdateUserUseCase = object : UpdateUserUseCase(
		users = dummyUsers(),
		roles = dummyRoles(),
		audit = dummyAudit(),
		clock = Clock.systemUTC(),
	) {
		override fun update(command: UpdateUserCommand): UpdateUserResult = exec(command)
	}

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
}

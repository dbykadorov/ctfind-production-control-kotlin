package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEventType
import com.ctfind.productioncontrol.auth.domain.Role
import com.ctfind.productioncontrol.auth.domain.UserAccount
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CreateUserUseCaseTests {

	private val clock = Clock.fixed(Instant.parse("2026-04-30T12:00:00Z"), ZoneOffset.UTC)
	private val encoder = BCryptPasswordEncoder()

	@Test
	fun `creates enabled user with selected roles and audit`() {
		val users = CreateUserInMemoryUsers()
		val roles = CreateUserInMemoryRoles(
			setOf(
				role(ADMIN_ROLE_CODE, "Administrator"),
				role(WAREHOUSE_ROLE_CODE, "Warehouse"),
			),
		)
		val audit = CreateUserInMemoryAudit()
		val useCase = CreateUserUseCase(users, roles, audit, encoder, clock)

		val result = useCase.create(
			CreateUserCommand(
				login = " warehouse.demo ",
				displayName = " Warehouse Demo ",
				initialPassword = "demo",
				roleCodes = setOf(WAREHOUSE_ROLE_CODE),
				actorRoleCodes = setOf(ADMIN_ROLE_CODE),
				actorLogin = "admin",
				actorUserId = UUID.fromString("30000000-0000-0000-0000-000000000003"),
			),
		)

		val success = assertIs<CreateUserResult.Success>(result)
		assertEquals("warehouse.demo", success.user.login)
		assertEquals("Warehouse Demo", success.user.displayName)
		assertEquals(WAREHOUSE_ROLE_CODE, success.user.roles.single().code)

		val stored = users.findByLogin("warehouse.demo")
		assertNotNull(stored)
		assertTrue(stored.enabled)
		assertTrue(encoder.matches("demo", stored.passwordHash))
		assertEquals(setOf(WAREHOUSE_ROLE_CODE), stored.roleCodes)
		assertEquals(AuthenticationAuditEventType.USER_CREATED, audit.events.single().eventType)
	}

	@Test
	fun `forbids non-admin actor`() {
		val useCase = CreateUserUseCase(
			users = CreateUserInMemoryUsers(),
			roles = CreateUserInMemoryRoles(setOf(role(WAREHOUSE_ROLE_CODE, "Warehouse"))),
			audit = CreateUserInMemoryAudit(),
			passwordEncoder = encoder,
			clock = clock,
		)

		val result = useCase.create(
			CreateUserCommand(
				login = "warehouse.demo",
				displayName = "Warehouse Demo",
				initialPassword = "demo",
				roleCodes = setOf(WAREHOUSE_ROLE_CODE),
				actorRoleCodes = setOf("ORDER_MANAGER"),
				actorLogin = "manager",
				actorUserId = null,
			),
		)

		assertIs<CreateUserResult.Forbidden>(result)
	}

	@Test
	fun `rejects duplicate login`() {
		val users = CreateUserInMemoryUsers().apply {
			save(
				UserAccount(
					id = UUID.randomUUID(),
					login = "warehouse.demo",
					displayName = "Existing",
					passwordHash = "hash",
					enabled = true,
					roleCodes = setOf(WAREHOUSE_ROLE_CODE),
					createdAt = Instant.now(clock),
					updatedAt = Instant.now(clock),
				),
			)
		}
		val useCase = CreateUserUseCase(
			users = users,
			roles = CreateUserInMemoryRoles(setOf(role(WAREHOUSE_ROLE_CODE, "Warehouse"))),
			audit = CreateUserInMemoryAudit(),
			passwordEncoder = encoder,
			clock = clock,
		)

		val result = useCase.create(
			CreateUserCommand(
				login = "warehouse.demo",
				displayName = "Warehouse Demo",
				initialPassword = "demo",
				roleCodes = setOf(WAREHOUSE_ROLE_CODE),
				actorRoleCodes = setOf(ADMIN_ROLE_CODE),
				actorLogin = "admin",
				actorUserId = null,
			),
		)

		assertIs<CreateUserResult.DuplicateLogin>(result)
	}

	@Test
	fun `rejects unsupported role codes`() {
		val useCase = CreateUserUseCase(
			users = CreateUserInMemoryUsers(),
			roles = CreateUserInMemoryRoles(setOf(role(WAREHOUSE_ROLE_CODE, "Warehouse"))),
			audit = CreateUserInMemoryAudit(),
			passwordEncoder = encoder,
			clock = clock,
		)

		val result = useCase.create(
			CreateUserCommand(
				login = "warehouse.demo",
				displayName = "Warehouse Demo",
				initialPassword = "demo",
				roleCodes = setOf("UNKNOWN_ROLE"),
				actorRoleCodes = setOf(ADMIN_ROLE_CODE),
				actorLogin = "admin",
				actorUserId = null,
			),
		)

		val invalid = assertIs<CreateUserResult.InvalidRoles>(result)
		assertEquals(setOf("UNKNOWN_ROLE"), invalid.roleCodes)
	}

	@Test
	fun `rejects empty initial password`() {
		val useCase = CreateUserUseCase(
			users = CreateUserInMemoryUsers(),
			roles = CreateUserInMemoryRoles(setOf(role(WAREHOUSE_ROLE_CODE, "Warehouse"))),
			audit = CreateUserInMemoryAudit(),
			passwordEncoder = encoder,
			clock = clock,
		)

		val result = useCase.create(
			CreateUserCommand(
				login = "warehouse.demo",
				displayName = "Warehouse Demo",
				initialPassword = "",
				roleCodes = setOf(WAREHOUSE_ROLE_CODE),
				actorRoleCodes = setOf(ADMIN_ROLE_CODE),
				actorLogin = "admin",
				actorUserId = null,
			),
		)

		val validation = assertIs<CreateUserResult.ValidationError>(result)
		assertEquals("initialPassword", validation.field)
	}

	@Test
	fun `allows assigning ADMIN role`() {
		val useCase = CreateUserUseCase(
			users = CreateUserInMemoryUsers(),
			roles = CreateUserInMemoryRoles(
				setOf(
					role(ADMIN_ROLE_CODE, "Administrator"),
					role(WAREHOUSE_ROLE_CODE, "Warehouse"),
				),
			),
			audit = CreateUserInMemoryAudit(),
			passwordEncoder = encoder,
			clock = clock,
		)

		val result = useCase.create(
			CreateUserCommand(
				login = "admin.backup",
				displayName = "Backup Admin",
				initialPassword = "demo",
				roleCodes = setOf(ADMIN_ROLE_CODE),
				actorRoleCodes = setOf(ADMIN_ROLE_CODE),
				actorLogin = "admin",
				actorUserId = null,
			),
		)

		val success = assertIs<CreateUserResult.Success>(result)
		assertEquals(ADMIN_ROLE_CODE, success.user.roles.single().code)
	}
}

private fun role(code: String, name: String): Role =
	Role(
		id = UUID.randomUUID(),
		code = code,
		name = name,
		createdAt = Instant.parse("2026-04-30T12:00:00Z"),
	)

private class CreateUserInMemoryUsers : UserAccountPort {
	private val users = linkedMapOf<String, UserAccount>()

	override fun findByLogin(login: String): UserAccount? = users[login.trim().lowercase()]

	override fun save(user: UserAccount): UserAccount {
		users[user.normalizedLogin] = user
		return user
	}

	override fun existsEnabledWithRole(roleCode: String): Boolean =
		users.values.any { it.enabled && roleCode.uppercase() in it.roleCodes }
}

private class CreateUserInMemoryRoles(initial: Set<Role>) : RolePort {
	private val roles = initial.associateBy { it.code.uppercase() }.toMutableMap()

	override fun findByCode(code: String): Role? = roles[code.uppercase()]

	override fun save(role: Role): Role {
		roles[role.code.uppercase()] = role
		return role
	}

	override fun findAllByCodes(codes: Set<String>): List<Role> =
		codes.mapNotNull { roles[it.uppercase()] }
}

private class CreateUserInMemoryAudit : AuthenticationAuditPort {
	val events = mutableListOf<AuthenticationAuditEvent>()

	override fun record(event: AuthenticationAuditEvent): AuthenticationAuditEvent {
		events += event
		return event
	}
}

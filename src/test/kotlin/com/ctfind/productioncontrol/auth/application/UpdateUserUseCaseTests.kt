package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEvent
import com.ctfind.productioncontrol.auth.domain.AuthenticationAuditEventType
import com.ctfind.productioncontrol.auth.domain.Role
import com.ctfind.productioncontrol.auth.domain.UserAccount
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UpdateUserUseCaseTests {

	private val clock = Clock.fixed(Instant.parse("2026-04-30T16:00:00Z"), ZoneOffset.UTC)
	private val adminId = UUID.fromString("10000000-0000-0000-0000-000000000001")
	private val userId = UUID.fromString("10000000-0000-0000-0000-000000000002")

	@Test
	fun `updates display name and roles`() {
		val users = InMemoryUsers(
			setOf(
				adminUser(id = adminId, login = "admin", roles = setOf(ADMIN_ROLE_CODE)),
				regularUser(id = userId, login = "worker", roles = setOf(WAREHOUSE_ROLE_CODE)),
			),
		)
		val roles = InMemoryRoles(
			setOf(
				role(ADMIN_ROLE_CODE, "Administrator"),
				role(WAREHOUSE_ROLE_CODE, "Warehouse"),
				role(PRODUCTION_EXECUTOR_ROLE_CODE, "Production Executor"),
			),
		)
		val audit = InMemoryAudit()
		val useCase = UpdateUserUseCase(users, roles, audit, clock)

		val result = useCase.update(
			UpdateUserCommand(
				userId = userId,
				displayName = " Worker Updated ",
				roleCodes = setOf(" warehouse ", "PRODUCTION_EXECUTOR", "WAREHOUSE"),
				actorRoleCodes = setOf(ADMIN_ROLE_CODE),
				actorLogin = "admin",
				actorUserId = adminId,
			),
		)

		val success = assertIs<UpdateUserResult.Success>(result)
		assertEquals("Worker Updated", success.user.displayName)
		assertEquals(setOf(PRODUCTION_EXECUTOR_ROLE_CODE, WAREHOUSE_ROLE_CODE), success.user.roles.map { it.code }.toSet())

		val stored = users.findById(userId)
		assertNotNull(stored)
		assertEquals("Worker Updated", stored.displayName)
		assertEquals(setOf(PRODUCTION_EXECUTOR_ROLE_CODE, WAREHOUSE_ROLE_CODE), stored.roleCodes)
	}

	@Test
	fun `returns forbidden for non-admin actor`() {
		val useCase = UpdateUserUseCase(
			users = InMemoryUsers(setOf(regularUser(userId, "worker", setOf(WAREHOUSE_ROLE_CODE)))),
			roles = InMemoryRoles(setOf(role(WAREHOUSE_ROLE_CODE, "Warehouse"))),
			audit = InMemoryAudit(),
			clock = clock,
		)

		val result = useCase.update(
			UpdateUserCommand(
				userId = userId,
				displayName = "Worker Updated",
				roleCodes = setOf(WAREHOUSE_ROLE_CODE),
				actorRoleCodes = setOf(WAREHOUSE_ROLE_CODE),
				actorLogin = "worker",
				actorUserId = userId,
			),
		)

		assertIs<UpdateUserResult.Forbidden>(result)
	}

	@Test
	fun `returns not found for missing target`() {
		val useCase = UpdateUserUseCase(
			users = InMemoryUsers(emptySet()),
			roles = InMemoryRoles(setOf(role(WAREHOUSE_ROLE_CODE, "Warehouse"))),
			audit = InMemoryAudit(),
			clock = clock,
		)

		val result = useCase.update(
			UpdateUserCommand(
				userId = userId,
				displayName = "Worker Updated",
				roleCodes = setOf(WAREHOUSE_ROLE_CODE),
				actorRoleCodes = setOf(ADMIN_ROLE_CODE),
				actorLogin = "admin",
				actorUserId = adminId,
			),
		)

		assertIs<UpdateUserResult.NotFound>(result)
	}

	@Test
	fun `returns validation error for blank display name`() {
		val useCase = UpdateUserUseCase(
			users = InMemoryUsers(setOf(regularUser(userId, "worker", setOf(WAREHOUSE_ROLE_CODE)))),
			roles = InMemoryRoles(setOf(role(WAREHOUSE_ROLE_CODE, "Warehouse"))),
			audit = InMemoryAudit(),
			clock = clock,
		)

		val result = useCase.update(
			UpdateUserCommand(
				userId = userId,
				displayName = "   ",
				roleCodes = setOf(WAREHOUSE_ROLE_CODE),
				actorRoleCodes = setOf(ADMIN_ROLE_CODE),
				actorLogin = "admin",
				actorUserId = adminId,
			),
		)

		val validation = assertIs<UpdateUserResult.ValidationError>(result)
		assertEquals("displayName", validation.field)
	}

	@Test
	fun `returns invalid roles for unsupported role code`() {
		val useCase = UpdateUserUseCase(
			users = InMemoryUsers(setOf(regularUser(userId, "worker", setOf(WAREHOUSE_ROLE_CODE)))),
			roles = InMemoryRoles(setOf(role(WAREHOUSE_ROLE_CODE, "Warehouse"))),
			audit = InMemoryAudit(),
			clock = clock,
		)

		val result = useCase.update(
			UpdateUserCommand(
				userId = userId,
				displayName = "Worker Updated",
				roleCodes = setOf("UNKNOWN_ROLE"),
				actorRoleCodes = setOf(ADMIN_ROLE_CODE),
				actorLogin = "admin",
				actorUserId = adminId,
			),
		)

		val invalid = assertIs<UpdateUserResult.InvalidRoles>(result)
		assertEquals(setOf("UNKNOWN_ROLE"), invalid.roleCodes)
	}

	@Test
	fun `blocks removing admin role from last active admin`() {
		val users = InMemoryUsers(
			setOf(
				adminUser(id = adminId, login = "admin", roles = setOf(ADMIN_ROLE_CODE)),
			),
		)
		val roles = InMemoryRoles(
			setOf(
				role(ADMIN_ROLE_CODE, "Administrator"),
				role(WAREHOUSE_ROLE_CODE, "Warehouse"),
			),
		)
		val useCase = UpdateUserUseCase(users, roles, InMemoryAudit(), clock)

		val result = useCase.update(
			UpdateUserCommand(
				userId = adminId,
				displayName = "Admin",
				roleCodes = setOf(WAREHOUSE_ROLE_CODE),
				actorRoleCodes = setOf(ADMIN_ROLE_CODE),
				actorLogin = "admin",
				actorUserId = adminId,
			),
		)

		assertIs<UpdateUserResult.LastAdminRoleRemovalForbidden>(result)
	}

	@Test
	fun `records update audit event with field deltas`() {
		val users = InMemoryUsers(
			setOf(
				adminUser(id = adminId, login = "admin", roles = setOf(ADMIN_ROLE_CODE)),
				regularUser(id = userId, login = "worker", roles = setOf(WAREHOUSE_ROLE_CODE)),
			),
		)
		val roles = InMemoryRoles(
			setOf(
				role(ADMIN_ROLE_CODE, "Administrator"),
				role(WAREHOUSE_ROLE_CODE, "Warehouse"),
				role(PRODUCTION_EXECUTOR_ROLE_CODE, "Production Executor"),
			),
		)
		val audit = InMemoryAudit()
		val useCase = UpdateUserUseCase(users, roles, audit, clock)

		val result = useCase.update(
			UpdateUserCommand(
				userId = userId,
				displayName = "Worker Updated",
				roleCodes = setOf(PRODUCTION_EXECUTOR_ROLE_CODE),
				actorRoleCodes = setOf(ADMIN_ROLE_CODE),
				actorLogin = "admin",
				actorUserId = adminId,
			),
		)

		assertIs<UpdateUserResult.Success>(result)
		val event = audit.events.single()
		assertEquals(AuthenticationAuditEventType.USER_UPDATED, event.eventType)
		assertEquals("admin", event.login)
		assertTrue(event.details?.contains("target_login=worker") == true)
		assertTrue(event.details?.contains("roles_added=PRODUCTION_EXECUTOR") == true)
		assertTrue(event.details?.contains("roles_removed=WAREHOUSE") == true)
		assertFalse(event.details?.contains("password", ignoreCase = true) == true)
	}
}

private fun role(code: String, name: String): Role =
	Role(
		id = UUID.randomUUID(),
		code = code,
		name = name,
		createdAt = Instant.parse("2026-04-30T16:00:00Z"),
	)

private fun adminUser(id: UUID, login: String, roles: Set<String>): UserAccount =
	UserAccount(
		id = id,
		login = login,
		displayName = "Admin",
		passwordHash = "hash",
		enabled = true,
		roleCodes = roles,
		createdAt = Instant.parse("2026-04-30T10:00:00Z"),
		updatedAt = Instant.parse("2026-04-30T10:00:00Z"),
	)

private fun regularUser(id: UUID, login: String, roles: Set<String>): UserAccount =
	UserAccount(
		id = id,
		login = login,
		displayName = "Worker",
		passwordHash = "hash",
		enabled = true,
		roleCodes = roles,
		createdAt = Instant.parse("2026-04-30T10:00:00Z"),
		updatedAt = Instant.parse("2026-04-30T10:00:00Z"),
	)

private class InMemoryUsers(initial: Set<UserAccount>) : UserAccountPort {
	private val usersById = initial.associateBy { it.id }.toMutableMap()

	override fun findByLogin(login: String): UserAccount? =
		usersById.values.firstOrNull { it.normalizedLogin == login.trim().lowercase() }

	override fun findById(id: UUID): UserAccount? = usersById[id]

	override fun save(user: UserAccount): UserAccount {
		usersById[user.id] = user
		return user
	}

	override fun existsEnabledWithRole(roleCode: String): Boolean =
		usersById.values.any { it.enabled && roleCode.uppercase() in it.roleCodes }

	override fun countEnabledWithRole(roleCode: String): Long =
		usersById.values.count { it.enabled && roleCode.uppercase() in it.roleCodes }.toLong()
}

private class InMemoryRoles(initial: Set<Role>) : RolePort {
	private val roles = initial.associateBy { it.code.uppercase() }.toMutableMap()

	override fun findByCode(code: String): Role? = roles[code.uppercase()]

	override fun save(role: Role): Role {
		roles[role.code.uppercase()] = role
		return role
	}

	override fun findAllByCodes(codes: Set<String>): List<Role> =
		codes.mapNotNull { roles[it.uppercase()] }
}

private class InMemoryAudit : AuthenticationAuditPort {
	val events = mutableListOf<AuthenticationAuditEvent>()

	override fun record(event: AuthenticationAuditEvent): AuthenticationAuditEvent {
		events += event
		return event
	}
}

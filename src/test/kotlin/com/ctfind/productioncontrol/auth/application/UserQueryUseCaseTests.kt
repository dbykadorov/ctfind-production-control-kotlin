package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UserQueryUseCaseTests {

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
	fun `ADMIN role delegates to port and returns user list`() {
		val uc = useCase(returnUsers = listOf(sampleUser1, sampleUser2))
		val result = uc.search(search = null, limit = 50, roleCodes = setOf(ADMIN_ROLE_CODE))

		val success = assertIs<UserQueryResult.Success>(result)
		assertEquals(2, success.users.size)
		assertEquals(sampleUser1.id, success.users[0].id)
		assertEquals(sampleUser2.id, success.users[1].id)
	}

	@Test
	fun `non-ADMIN role returns Forbidden`() {
		val uc = useCase(returnUsers = listOf(sampleUser1))
		val result = uc.search(search = null, limit = 50, roleCodes = setOf("ORDER_MANAGER"))

		assertIs<UserQueryResult.Forbidden>(result)
	}

	@Test
	fun `empty roles returns Forbidden`() {
		val uc = useCase(returnUsers = listOf(sampleUser1))
		val result = uc.search(search = null, limit = 50, roleCodes = emptySet())

		assertIs<UserQueryResult.Forbidden>(result)
	}

	@Test
	fun `search param is forwarded to port`() {
		var capturedSearch: String? = "NOT_CALLED"
		val port = object : UserQueryPort {
			override fun searchUsers(search: String?, limit: Int): List<UserSummary> {
				capturedSearch = search
				return listOf(sampleUser1)
			}
		}
		val uc = UserQueryUseCase(port)

		uc.search(search = "иван", limit = 50, roleCodes = setOf(ADMIN_ROLE_CODE))

		assertEquals("иван", capturedSearch)
	}

	@Test
	fun `limit param is forwarded to port`() {
		var capturedLimit: Int? = null
		val port = object : UserQueryPort {
			override fun searchUsers(search: String?, limit: Int): List<UserSummary> {
				capturedLimit = limit
				return emptyList()
			}
		}
		val uc = UserQueryUseCase(port)

		uc.search(search = null, limit = 10, roleCodes = setOf(ADMIN_ROLE_CODE))

		assertEquals(10, capturedLimit)
	}

	@Test
	fun `empty result from port returns empty list`() {
		val uc = useCase(returnUsers = emptyList())
		val result = uc.search(search = "nonexistent", limit = 50, roleCodes = setOf(ADMIN_ROLE_CODE))

		val success = assertIs<UserQueryResult.Success>(result)
		assertEquals(0, success.users.size)
	}

	private fun useCase(returnUsers: List<UserSummary>): UserQueryUseCase {
		val port = object : UserQueryPort {
			override fun searchUsers(search: String?, limit: Int): List<UserSummary> = returnUsers
		}
		return UserQueryUseCase(port)
	}
}

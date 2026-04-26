package com.ctfind.productioncontrol.auth.adapter.persistence

import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.assertTrue

class AuthMigrationTests {

	@Test
	fun `auth migration creates required tables and uniqueness rules`() {
		val migration = Path("src/main/resources/db/migration/V2__create_auth_tables.sql")
			.readText()
			.lowercase()
		val joinDefaults = Path("src/main/resources/db/migration/V3__default_user_role_created_at.sql")
			.readText()
			.lowercase()

		assertTrue(migration.contains("create table app_user"))
		assertTrue(migration.contains("create table app_role"))
		assertTrue(migration.contains("create table app_user_role"))
		assertTrue(migration.contains("create table auth_audit_event"))
		assertTrue(migration.contains("unique"))
		assertTrue(migration.contains("password_hash"))
		assertTrue(joinDefaults.contains("alter table app_user_role"))
		assertTrue(joinDefaults.contains("set default"))
	}
}

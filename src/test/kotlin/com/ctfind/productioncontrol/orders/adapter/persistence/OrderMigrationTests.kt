package com.ctfind.productioncontrol.orders.adapter.persistence

import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.assertTrue

class OrderMigrationTests {

	@Test
	fun `order migration creates order tables, numbering, history, and constraints`() {
		val migration = Path("src/main/resources/db/migration/V4__create_order_tables.sql")
			.readText()
			.lowercase()

		assertTrue(migration.contains("create sequence customer_order_number_seq"))
		assertTrue(migration.contains("create table customer"))
		assertTrue(migration.contains("create table customer_order"))
		assertTrue(migration.contains("create table customer_order_item"))
		assertTrue(migration.contains("create table order_status_change"))
		assertTrue(migration.contains("create table order_change_diff"))
		assertTrue(migration.contains("create table order_audit_event"))
		assertTrue(migration.contains("unique"))
		assertTrue(migration.contains("version bigint not null"))
		assertTrue(migration.contains("foreign key"))
	}
}

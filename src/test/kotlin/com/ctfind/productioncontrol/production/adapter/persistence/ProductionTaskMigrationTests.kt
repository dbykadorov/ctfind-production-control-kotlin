package com.ctfind.productioncontrol.production.adapter.persistence

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class ProductionTaskMigrationTests {

	private val migration = Files.readString(Path.of("src/main/resources/db/migration/V5__create_production_task_tables.sql"))

	@Test
	fun `migration creates production task tables and number sequence`() {
		assertTrue(migration.contains("create sequence production_task_number_seq"))
		assertTrue(migration.contains("create table production_task"))
		assertTrue(migration.contains("create table production_task_history_event"))
		assertTrue(migration.contains("create table production_task_audit_event"))
	}

	@Test
	fun `migration links production tasks to order item executor and actor records`() {
		assertTrue(migration.contains("references customer_order(id)"))
		assertTrue(migration.contains("references customer_order_item(id)"))
		assertTrue(migration.contains("references app_user(id)"))
	}

	@Test
	fun `migration indexes production task query fields`() {
		assertTrue(migration.contains("idx_production_task_order_id"))
		assertTrue(migration.contains("idx_production_task_order_item_id"))
		assertTrue(migration.contains("idx_production_task_status"))
		assertTrue(migration.contains("idx_production_task_executor_user_id"))
		assertTrue(migration.contains("idx_production_task_planned_finish_date"))
		assertTrue(migration.contains("idx_production_task_history_task_event_at"))
	}
}

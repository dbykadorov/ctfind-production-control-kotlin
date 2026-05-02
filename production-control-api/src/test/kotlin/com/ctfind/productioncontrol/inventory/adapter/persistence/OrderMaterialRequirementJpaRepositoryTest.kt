package com.ctfind.productioncontrol.inventory.adapter.persistence

import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertTrue

class OrderMaterialRequirementJpaRepositoryTest {

    private val migrationSource = Path.of(
        "src/main/resources/db/migration/V9__bom_and_consumption.sql",
    ).readText()

    private val repositorySource = Path.of(
        "src/main/kotlin/com/ctfind/productioncontrol/inventory/adapter/persistence/InventoryJpaRepositories.kt",
    ).readText()

    @Test
    fun `migration enforces unique order-material pair`() {
        assertTrue(
            migrationSource.contains("UNIQUE (order_id, material_id)"),
            "V9 migration must enforce UNIQUE(order_id, material_id)",
        )
    }

    @Test
    fun `repository keeps basic CRUD and bom lookups`() {
        assertTrue(
            repositorySource.contains(
                "interface OrderMaterialRequirementJpaRepository : JpaRepository<OrderMaterialRequirementEntity, UUID>",
            ),
            "Repository must extend JpaRepository for base CRUD operations",
        )
        assertTrue(
            repositorySource.contains(
                "fun findByOrderIdOrderByCreatedAtDesc(orderId: UUID): List<OrderMaterialRequirementEntity>",
            ),
            "Repository must support listing BOM lines by order",
        )
        assertTrue(
            repositorySource.contains(
                "fun findByOrderIdAndMaterialId(orderId: UUID, materialId: UUID): OrderMaterialRequirementEntity?",
            ),
            "Repository must support lookup by order/material pair",
        )
        assertTrue(
            repositorySource.contains(
                "fun existsByOrderIdAndMaterialId(orderId: UUID, materialId: UUID): Boolean",
            ),
            "Repository must support duplicate guard lookup",
        )
    }
}

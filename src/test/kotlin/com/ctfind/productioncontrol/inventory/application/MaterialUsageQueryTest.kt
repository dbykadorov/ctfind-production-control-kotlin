package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import com.ctfind.productioncontrol.inventory.domain.OrderMaterialRequirement
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class MaterialUsageQueryTest {
    private val materials = mockk<MaterialPort>()
    private val movements = mockk<StockMovementPort>()
    private val requirements = mockk<OrderMaterialRequirementPort>()
    private val orders = mockk<OrderLookupPort>()
    private val query = InventoryQueryUseCase(materials, movements, requirements, orders)

    private fun material(id: UUID, name: String, unit: MeasurementUnit = MeasurementUnit.PIECE) = Material(
        id = id,
        name = name,
        unit = unit,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Test
    fun `returns null when order is missing`() {
        val orderId = UUID.randomUUID()
        every { orders.findOrderSummary(orderId) } returns null
        val result = query.getMaterialUsage(orderId)
        assertEquals(null, result)
    }

    @Test
    fun `returns empty rows when order has no BOM`() {
        val orderId = UUID.randomUUID()
        every { orders.findOrderSummary(orderId) } returns InventoryOrderSummary(
            id = orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )
        every { requirements.findByOrderIdOrderByCreatedAtDesc(orderId) } returns emptyList()
        every { movements.sumConsumedByOrder(orderId) } returns emptyMap()

        val usage = requireNotNull(query.getMaterialUsage(orderId))
        assertTrue(usage.rows.isEmpty())
    }

    @Test
    fun `calculates remaining and overconsumption correctly`() {
        val orderId = UUID.randomUUID()
        val m1 = UUID.randomUUID()
        val m2 = UUID.randomUUID()
        val now = Instant.now()
        every { orders.findOrderSummary(orderId) } returns InventoryOrderSummary(
            id = orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )
        every { requirements.findByOrderIdOrderByCreatedAtDesc(orderId) } returns listOf(
            OrderMaterialRequirement(
                id = UUID.randomUUID(),
                orderId = orderId,
                materialId = m1,
                quantity = BigDecimal("10"),
                comment = null,
                createdAt = now,
                updatedAt = now,
            ),
            OrderMaterialRequirement(
                id = UUID.randomUUID(),
                orderId = orderId,
                materialId = m2,
                quantity = BigDecimal("5"),
                comment = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        every { movements.sumConsumedByOrder(orderId) } returns mapOf(
            m1 to BigDecimal("4"),
            m2 to BigDecimal("8"),
        )
        every { materials.findById(m1) } returns material(m1, "Фанера", MeasurementUnit.SQUARE_METER)
        every { materials.findById(m2) } returns material(m2, "Клей", MeasurementUnit.LITER)

        val usage = requireNotNull(query.getMaterialUsage(orderId))
        val byMaterial = usage.rows.associateBy { it.materialId }

        assertEquals(BigDecimal("6"), byMaterial[m1]?.remainingToConsume)
        assertEquals(BigDecimal.ZERO, byMaterial[m1]?.overconsumption)
        assertEquals(BigDecimal.ZERO, byMaterial[m2]?.remainingToConsume)
        assertEquals(BigDecimal("3"), byMaterial[m2]?.overconsumption)
    }
}

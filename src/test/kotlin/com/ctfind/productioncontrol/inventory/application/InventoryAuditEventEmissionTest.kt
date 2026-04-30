package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import com.ctfind.productioncontrol.inventory.domain.MovementType
import com.ctfind.productioncontrol.inventory.domain.OrderMaterialRequirement
import com.ctfind.productioncontrol.inventory.domain.StockMovement
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class InventoryAuditEventEmissionTest {

    @Test
    fun `add bom emits BOM_LINE_ADDED with actor and metadata`() {
        val materials = mockk<MaterialPort>()
        val requirements = mockk<OrderMaterialRequirementPort>()
        val orders = mockk<OrderLookupPort>()
        val audit = mockk<InventoryAuditPort>()
        val useCase = AddBomLineUseCase(materials, requirements, orders, audit)

        val actor = actor("ORDER_MANAGER")
        val orderId = UUID.randomUUID()
        val materialId = UUID.randomUUID()
        val command = AddBomLineCommand(
            orderId = orderId,
            materialId = materialId,
            quantity = BigDecimal("10"),
            comment = "На корпус",
            actor = actor,
        )

        every { orders.findOrderSummary(orderId) } returns orderSummary(orderId)
        every { materials.findById(materialId) } returns material(materialId)
        every { requirements.existsByOrderIdAndMaterialId(orderId, materialId) } returns false
        every { requirements.save(any()) } answers { firstArg() }
        val eventSlot = slot<InventoryAuditEvent>()
        every { audit.record(capture(eventSlot)) } answers { eventSlot.captured }

        val result = useCase.add(command)

        assertInstanceOf(InventoryMutationResult.Success::class.java, result)
        val event = eventSlot.captured
        assertEquals("BOM_LINE_ADDED", event.eventType)
        assertEquals(actor.userId, event.actorUserId)
        assertTrue(event.metadata?.contains("\"orderId\":\"$orderId\"") == true)
        assertTrue(event.metadata?.contains("\"materialId\":\"$materialId\"") == true)
        assertTrue(event.metadata?.contains("\"quantity\":\"10\"") == true)
    }

    @Test
    fun `update bom emits BOM_LINE_UPDATED with before-after diff metadata`() {
        val materials = mockk<MaterialPort>()
        val requirements = mockk<OrderMaterialRequirementPort>()
        val orders = mockk<OrderLookupPort>()
        val audit = mockk<InventoryAuditPort>()
        val useCase = UpdateBomLineUseCase(materials, requirements, orders, audit)

        val actor = actor("ORDER_MANAGER")
        val orderId = UUID.randomUUID()
        val lineId = UUID.randomUUID()
        val materialId = UUID.randomUUID()
        val existing = OrderMaterialRequirement(
            id = lineId,
            orderId = orderId,
            materialId = materialId,
            quantity = BigDecimal("10"),
            comment = "Старый комментарий",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val command = UpdateBomLineCommand(
            orderId = orderId,
            lineId = lineId,
            quantity = BigDecimal("12"),
            comment = "Новый комментарий",
            actor = actor,
        )

        every { orders.findOrderSummary(orderId) } returns orderSummary(orderId)
        every { requirements.findByLineIdAndOrderId(lineId, orderId) } returns existing
        every { materials.findById(materialId) } returns material(materialId)
        every { requirements.save(any()) } answers { firstArg() }
        val eventSlot = slot<InventoryAuditEvent>()
        every { audit.record(capture(eventSlot)) } answers { eventSlot.captured }

        val result = useCase.update(command)

        assertInstanceOf(InventoryMutationResult.Success::class.java, result)
        val event = eventSlot.captured
        assertEquals("BOM_LINE_UPDATED", event.eventType)
        assertEquals(actor.userId, event.actorUserId)
        assertTrue(event.metadata?.contains("\"before\":{\"quantity\":\"10\"") == true)
        assertTrue(event.metadata?.contains("\"comment\":\"Старый комментарий\"") == true)
        assertTrue(event.metadata?.contains("\"after\":{\"quantity\":\"12\"") == true)
        assertTrue(event.metadata?.contains("\"comment\":\"Новый комментарий\"") == true)
    }

    @Test
    fun `remove bom emits BOM_LINE_REMOVED with actor and quantity metadata`() {
        val materials = mockk<MaterialPort>()
        val requirements = mockk<OrderMaterialRequirementPort>()
        val movements = mockk<StockMovementPort>()
        val orders = mockk<OrderLookupPort>()
        val audit = mockk<InventoryAuditPort>()
        val useCase = RemoveBomLineUseCase(materials, requirements, movements, orders, audit)

        val actor = actor("ORDER_MANAGER")
        val orderId = UUID.randomUUID()
        val lineId = UUID.randomUUID()
        val materialId = UUID.randomUUID()
        val existing = OrderMaterialRequirement(
            id = lineId,
            orderId = orderId,
            materialId = materialId,
            quantity = BigDecimal("10"),
            comment = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val command = RemoveBomLineCommand(orderId = orderId, lineId = lineId, actor = actor)

        every { orders.findOrderSummary(orderId) } returns orderSummary(orderId)
        every { requirements.findByLineIdAndOrderId(lineId, orderId) } returns existing
        every { materials.findById(materialId) } returns material(materialId)
        every { movements.hasConsumption(orderId, materialId) } returns false
        every { requirements.deleteLineById(lineId) } returns Unit
        val eventSlot = slot<InventoryAuditEvent>()
        every { audit.record(capture(eventSlot)) } answers { eventSlot.captured }

        val result = useCase.remove(command)

        assertInstanceOf(InventoryMutationResult.Success::class.java, result)
        val event = eventSlot.captured
        assertEquals("BOM_LINE_REMOVED", event.eventType)
        assertEquals(actor.userId, event.actorUserId)
        assertTrue(event.metadata?.contains("\"orderId\":\"$orderId\"") == true)
        assertTrue(event.metadata?.contains("\"materialId\":\"$materialId\"") == true)
        assertTrue(event.metadata?.contains("\"quantity\":\"10\"") == true)
    }

    @Test
    fun `consume emits STOCK_CONSUMPTION with order and material metadata`() {
        val materials = mockk<MaterialPort>()
        val requirements = mockk<OrderMaterialRequirementPort>()
        val movements = mockk<StockMovementPort>()
        val orders = mockk<OrderLookupPort>()
        val audit = mockk<InventoryAuditPort>()
        val useCase = ConsumeStockUseCase(materials, requirements, movements, orders, audit)

        val actor = actor("WAREHOUSE")
        val orderId = UUID.randomUUID()
        val materialId = UUID.randomUUID()
        val command = ConsumeStockCommand(
            materialId = materialId,
            orderId = orderId,
            quantity = BigDecimal("7"),
            comment = "В цех",
            actor = actor,
        )

        every { materials.findByIdForUpdate(materialId) } returns material(materialId)
        every { orders.findOrderSummary(orderId) } returns orderSummary(orderId)
        every { requirements.existsByOrderIdAndMaterialId(orderId, materialId) } returns true
        every { movements.sumQuantityByMaterialId(materialId) } returns BigDecimal("50")
        every { movements.save(any()) } answers { firstArg<StockMovement>() }
        val eventSlot = slot<InventoryAuditEvent>()
        every { audit.record(capture(eventSlot)) } answers { eventSlot.captured }

        val result = useCase.consume(command)

        assertInstanceOf(InventoryMutationResult.Success::class.java, result)
        val event = eventSlot.captured
        assertEquals("STOCK_CONSUMPTION", event.eventType)
        assertEquals(actor.userId, event.actorUserId)
        assertTrue(event.metadata?.contains("\"orderId\":\"$orderId\"") == true)
        assertTrue(event.metadata?.contains("\"orderNumber\":\"ORD-1\"") == true)
        assertTrue(event.metadata?.contains("\"materialId\":\"$materialId\"") == true)
        assertTrue(event.metadata?.contains("\"quantity\":\"7\"") == true)
    }

    private fun actor(vararg roles: String) = AuthenticatedInventoryActor(
        userId = UUID.randomUUID(),
        login = "tester",
        displayName = "Tester",
        roleCodes = roles.toSet(),
    )

    private fun orderSummary(orderId: UUID) = InventoryOrderSummary(
        id = orderId,
        orderNumber = "ORD-1",
        customerName = "Acme",
        status = OrderStatus.IN_WORK,
    )

    private fun material(materialId: UUID) = Material(
        id = materialId,
        name = "Фанера",
        unit = MeasurementUnit.SQUARE_METER,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )
}

package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import com.ctfind.productioncontrol.inventory.domain.OrderMaterialRequirement
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class RemoveBomLineUseCaseTest {
    private val materials = mockk<MaterialPort>()
    private val requirements = mockk<OrderMaterialRequirementPort>()
    private val movements = mockk<StockMovementPort>()
    private val orders = mockk<OrderLookupPort>()
    private val audit = mockk<InventoryAuditPort>()
    private val useCase = RemoveBomLineUseCase(materials, requirements, movements, orders, audit)

    private fun actor(vararg roles: String) = AuthenticatedInventoryActor(
        userId = UUID.randomUUID(),
        login = "tester",
        displayName = "Tester",
        roleCodes = roles.toSet(),
    )

    private fun command(
        orderId: UUID = UUID.randomUUID(),
        lineId: UUID = UUID.randomUUID(),
        roles: Set<String> = setOf("ORDER_MANAGER"),
    ) = RemoveBomLineCommand(
        orderId = orderId,
        lineId = lineId,
        actor = actor(*roles.toTypedArray()),
    )

    @Test
    fun `forbidden without ORDER_MANAGER role`() {
        val result = useCase.remove(command(roles = setOf("WAREHOUSE")))
        assertInstanceOf(InventoryMutationResult.Forbidden::class.java, result)
    }

    @Test
    fun `returns BomLineNotFound when missing`() {
        val cmd = command()
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )
        every { requirements.findByLineIdAndOrderId(cmd.lineId, cmd.orderId) } returns null
        val result = useCase.remove(cmd)
        assertInstanceOf(InventoryMutationResult.BomLineNotFound::class.java, result)
    }

    @Test
    fun `returns OrderLocked for shipped order`() {
        val cmd = command()
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.SHIPPED,
        )
        val result = useCase.remove(cmd)
        assertInstanceOf(InventoryMutationResult.OrderLocked::class.java, result)
    }

    @Test
    fun `returns Conflict when consumption exists`() {
        val cmd = command()
        val materialId = UUID.randomUUID()
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )
        every { requirements.findByLineIdAndOrderId(cmd.lineId, cmd.orderId) } returns OrderMaterialRequirement(
            id = cmd.lineId,
            orderId = cmd.orderId,
            materialId = materialId,
            quantity = BigDecimal("10"),
            comment = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        every { materials.findById(materialId) } returns Material(
            id = materialId,
            name = "Фанера",
            unit = MeasurementUnit.SQUARE_METER,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        every { movements.hasConsumption(cmd.orderId, materialId) } returns true
        val result = useCase.remove(cmd)
        assertInstanceOf(InventoryMutationResult.Conflict::class.java, result)
    }

    @Test
    fun `success deletes line and records audit`() {
        val cmd = command()
        val materialId = UUID.randomUUID()
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )
        every { requirements.findByLineIdAndOrderId(cmd.lineId, cmd.orderId) } returns OrderMaterialRequirement(
            id = cmd.lineId,
            orderId = cmd.orderId,
            materialId = materialId,
            quantity = BigDecimal("10"),
            comment = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        every { materials.findById(materialId) } returns Material(
            id = materialId,
            name = "Фанера",
            unit = MeasurementUnit.SQUARE_METER,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        every { movements.hasConsumption(cmd.orderId, materialId) } returns false
        every { requirements.deleteLineById(cmd.lineId) } returns Unit
        every { audit.record(any()) } answers { firstArg() }

        val result = useCase.remove(cmd)

        assertInstanceOf(InventoryMutationResult.Success::class.java, result)
        verify { audit.record(match { it.eventType == "BOM_LINE_REMOVED" }) }
    }
}

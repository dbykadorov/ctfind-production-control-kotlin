package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import com.ctfind.productioncontrol.inventory.domain.MovementType
import com.ctfind.productioncontrol.inventory.domain.StockMovement
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class ConsumeStockUseCaseTest {
    private val materials = mockk<MaterialPort>()
    private val requirements = mockk<OrderMaterialRequirementPort>()
    private val movements = mockk<StockMovementPort>()
    private val orders = mockk<OrderLookupPort>()
    private val audit = mockk<InventoryAuditPort>()
    private val useCase = ConsumeStockUseCase(materials, requirements, movements, orders, audit)

    private fun actor(vararg roles: String) = AuthenticatedInventoryActor(
        userId = UUID.randomUUID(),
        login = "tester",
        displayName = "Tester",
        roleCodes = roles.toSet(),
    )

    private fun command(
        materialId: UUID = UUID.randomUUID(),
        orderId: UUID = UUID.randomUUID(),
        quantity: BigDecimal = BigDecimal("5"),
        roles: Set<String> = setOf("WAREHOUSE"),
    ) = ConsumeStockCommand(
        materialId = materialId,
        orderId = orderId,
        quantity = quantity,
        comment = "Выдано в цех",
        actor = actor(*roles.toTypedArray()),
    )

    private fun material(id: UUID) = Material(
        id = id,
        name = "Фанера",
        unit = MeasurementUnit.SQUARE_METER,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Test
    fun `forbidden without warehouse role`() {
        val result = useCase.consume(command(roles = setOf("ORDER_MANAGER")))
        assertInstanceOf(InventoryMutationResult.Forbidden::class.java, result)
    }

    @Test
    fun `returns MaterialNotFound when missing`() {
        val cmd = command()
        every { materials.findByIdForUpdate(cmd.materialId) } returns null
        val result = useCase.consume(cmd)
        assertInstanceOf(InventoryMutationResult.MaterialNotFound::class.java, result)
    }

    @Test
    fun `returns OrderNotFound when missing`() {
        val cmd = command()
        every { materials.findByIdForUpdate(cmd.materialId) } returns material(cmd.materialId)
        every { orders.findOrderSummary(cmd.orderId) } returns null
        val result = useCase.consume(cmd)
        assertInstanceOf(InventoryMutationResult.OrderNotFound::class.java, result)
    }

    @Test
    fun `returns OrderLocked for shipped order`() {
        val cmd = command()
        every { materials.findByIdForUpdate(cmd.materialId) } returns material(cmd.materialId)
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.SHIPPED,
        )
        val result = useCase.consume(cmd)
        assertInstanceOf(InventoryMutationResult.OrderLocked::class.java, result)
    }

    @Test
    fun `returns MaterialNotInBom when not linked to order`() {
        val cmd = command()
        every { materials.findByIdForUpdate(cmd.materialId) } returns material(cmd.materialId)
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )
        every { requirements.existsByOrderIdAndMaterialId(cmd.orderId, cmd.materialId) } returns false

        val result = useCase.consume(cmd)
        assertInstanceOf(InventoryMutationResult.MaterialNotInBom::class.java, result)
    }

    @Test
    fun `returns ValidationFailed when quantity is not positive`() {
        val result = useCase.consume(command(quantity = BigDecimal.ZERO))
        assertInstanceOf(InventoryMutationResult.ValidationFailed::class.java, result)
    }

    @Test
    fun `returns InsufficientStock with available value`() {
        val cmd = command(quantity = BigDecimal("6"))
        every { materials.findByIdForUpdate(cmd.materialId) } returns material(cmd.materialId)
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )
        every { requirements.existsByOrderIdAndMaterialId(cmd.orderId, cmd.materialId) } returns true
        every { movements.sumQuantityByMaterialId(cmd.materialId) } returns BigDecimal("3")

        val result = useCase.consume(cmd)
        val insufficient = assertInstanceOf(InventoryMutationResult.InsufficientStock::class.java, result)
        assertEquals(BigDecimal("3"), insufficient.available)
    }

    @Test
    fun `success records STOCK_CONSUMPTION audit event`() {
        val cmd = command(quantity = BigDecimal("5"))
        val saved = StockMovement(
            id = UUID.randomUUID(),
            materialId = cmd.materialId,
            movementType = MovementType.CONSUMPTION,
            quantity = cmd.quantity,
            comment = cmd.comment,
            orderId = cmd.orderId,
            actorUserId = cmd.actor.userId,
            actorDisplayName = cmd.actor.displayName,
            createdAt = Instant.now(),
        )
        every { materials.findByIdForUpdate(cmd.materialId) } returns material(cmd.materialId)
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )
        every { requirements.existsByOrderIdAndMaterialId(cmd.orderId, cmd.materialId) } returns true
        every { movements.sumQuantityByMaterialId(cmd.materialId) } returns BigDecimal("10")
        every { movements.save(any()) } returns saved
        val eventSlot = slot<InventoryAuditEvent>()
        every { audit.record(capture(eventSlot)) } answers { eventSlot.captured }

        val result = useCase.consume(cmd)

        assertInstanceOf(InventoryMutationResult.Success::class.java, result)
        verify { audit.record(match { it.eventType == "STOCK_CONSUMPTION" }) }
    }
}

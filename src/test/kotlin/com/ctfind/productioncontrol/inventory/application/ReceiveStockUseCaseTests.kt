package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import com.ctfind.productioncontrol.inventory.domain.MovementType
import com.ctfind.productioncontrol.inventory.domain.StockMovement
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

class ReceiveStockUseCaseTests {

    private val materialPort = mockk<MaterialPort>()
    private val movementPort = mockk<StockMovementPort>()
    private val auditPort = mockk<InventoryAuditPort>()
    private val useCase = ReceiveStockUseCase(materialPort, movementPort, auditPort)

    private fun actor(vararg roles: String) = AuthenticatedInventoryActor(
        userId = UUID.randomUUID(),
        login = "test",
        displayName = "Test User",
        roleCodes = roles.toSet(),
    )

    private fun material(id: UUID = UUID.randomUUID()) = Material(
        id = id,
        name = "Steel",
        unit = MeasurementUnit.KILOGRAM,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Test
    fun `returns Forbidden when actor lacks WAREHOUSE role`() {
        val cmd = ReceiveStockCommand(UUID.randomUUID(), BigDecimal("10"), null, actor("ORDER_MANAGER"))
        val result = useCase.receive(cmd)
        assertInstanceOf(InventoryMutationResult.Forbidden::class.java, result)
    }

    @Test
    fun `returns NotFound when material does not exist`() {
        val cmd = ReceiveStockCommand(UUID.randomUUID(), BigDecimal("10"), null, actor("WAREHOUSE"))
        every { materialPort.findById(any()) } returns null
        val result = useCase.receive(cmd)
        assertInstanceOf(InventoryMutationResult.NotFound::class.java, result)
    }

    @Test
    fun `returns ValidationFailed when quantity is zero`() {
        val id = UUID.randomUUID()
        val cmd = ReceiveStockCommand(id, BigDecimal.ZERO, null, actor("WAREHOUSE"))
        every { materialPort.findById(id) } returns material(id)
        val result = useCase.receive(cmd)
        assertInstanceOf(InventoryMutationResult.ValidationFailed::class.java, result)
    }

    @Test
    fun `returns ValidationFailed when quantity is negative`() {
        val id = UUID.randomUUID()
        val cmd = ReceiveStockCommand(id, BigDecimal("-5"), null, actor("WAREHOUSE"))
        every { materialPort.findById(id) } returns material(id)
        val result = useCase.receive(cmd)
        assertInstanceOf(InventoryMutationResult.ValidationFailed::class.java, result)
    }

    @Test
    fun `returns Success and creates StockMovement with correct quantity`() {
        val id = UUID.randomUUID()
        val mat = material(id)
        val cmd = ReceiveStockCommand(id, BigDecimal("25.5"), "delivery note", actor("WAREHOUSE"))
        val savedMovement = StockMovement(
            id = UUID.randomUUID(),
            materialId = id,
            movementType = MovementType.RECEIPT,
            quantity = BigDecimal("25.5"),
            comment = "delivery note",
            orderId = null,
            actorUserId = cmd.actor.userId,
            actorDisplayName = cmd.actor.displayName,
            createdAt = Instant.now(),
        )

        every { materialPort.findById(id) } returns mat
        every { movementPort.save(any()) } returns savedMovement
        val eventSlot = slot<InventoryAuditEvent>()
        every { auditPort.record(capture(eventSlot)) } answers { eventSlot.captured }

        val result = useCase.receive(cmd)

        assertInstanceOf(InventoryMutationResult.Success::class.java, result)
        val view = (result as InventoryMutationResult.Success).value
        assertEquals(BigDecimal("25.5"), view.quantity)
        assertEquals(MovementType.RECEIPT, view.movementType)
    }

    @Test
    fun `records STOCK_RECEIPT audit event on success`() {
        val id = UUID.randomUUID()
        val mat = material(id)
        val cmd = ReceiveStockCommand(id, BigDecimal("10"), null, actor("WAREHOUSE"))
        val savedMovement = StockMovement(
            id = UUID.randomUUID(),
            materialId = id,
            movementType = MovementType.RECEIPT,
            quantity = BigDecimal("10"),
            comment = null,
            orderId = null,
            actorUserId = cmd.actor.userId,
            actorDisplayName = cmd.actor.displayName,
            createdAt = Instant.now(),
        )

        every { materialPort.findById(id) } returns mat
        every { movementPort.save(any()) } returns savedMovement
        every { auditPort.record(any()) } answers { firstArg() }

        useCase.receive(cmd)

        verify { auditPort.record(match { it.eventType == "STOCK_RECEIPT" }) }
    }
}

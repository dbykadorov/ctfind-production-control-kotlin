package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class DeleteMaterialUseCaseTests {

    private val materialPort = mockk<MaterialPort>()
    private val movementPort = mockk<StockMovementPort>()
    private val auditPort = mockk<InventoryAuditPort>()
    private val useCase = DeleteMaterialUseCase(materialPort, movementPort, auditPort)

    private fun actor(vararg roles: String) = AuthenticatedInventoryActor(
        userId = UUID.randomUUID(), login = "test", displayName = "Test", roleCodes = roles.toSet(),
    )

    private fun material(id: UUID = UUID.randomUUID()) = Material(
        id = id, name = "Test", unit = MeasurementUnit.PIECE,
        createdAt = Instant.now(), updatedAt = Instant.now(),
    )

    @Test
    fun `returns Forbidden when actor lacks WAREHOUSE role`() {
        val cmd = DeleteMaterialCommand(UUID.randomUUID(), actor("ORDER_MANAGER"))
        assertInstanceOf(InventoryMutationResult.Forbidden::class.java, useCase.delete(cmd))
    }

    @Test
    fun `returns NotFound when material does not exist`() {
        val id = UUID.randomUUID()
        val cmd = DeleteMaterialCommand(id, actor("WAREHOUSE"))
        every { materialPort.findById(id) } returns null
        assertInstanceOf(InventoryMutationResult.NotFound::class.java, useCase.delete(cmd))
    }

    @Test
    fun `returns Conflict when material has movements`() {
        val id = UUID.randomUUID()
        val cmd = DeleteMaterialCommand(id, actor("WAREHOUSE"))
        every { materialPort.findById(id) } returns material(id)
        every { materialPort.hasMovements(id) } returns true
        assertInstanceOf(InventoryMutationResult.Conflict::class.java, useCase.delete(cmd))
    }

    @Test
    fun `returns Success and records audit when no movements`() {
        val id = UUID.randomUUID()
        val cmd = DeleteMaterialCommand(id, actor("WAREHOUSE"))
        every { materialPort.findById(id) } returns material(id)
        every { materialPort.hasMovements(id) } returns false
        every { materialPort.deleteById(id) } returns Unit
        every { auditPort.record(any()) } answers { firstArg() }

        val result = useCase.delete(cmd)
        assertInstanceOf(InventoryMutationResult.Success::class.java, result)
        verify { auditPort.record(match { it.eventType == "MATERIAL_DELETED" }) }
    }
}

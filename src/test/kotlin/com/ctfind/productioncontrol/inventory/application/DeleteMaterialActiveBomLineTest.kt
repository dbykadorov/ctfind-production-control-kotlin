package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class DeleteMaterialActiveBomLineTest {
    private val materialPort = mockk<MaterialPort>()
    private val requirementPort = mockk<OrderMaterialRequirementPort>()
    private val auditPort = mockk<InventoryAuditPort>()
    private val useCase = DeleteMaterialUseCase(materialPort, requirementPort, auditPort)

    private fun actor(vararg roles: String) = AuthenticatedInventoryActor(
        userId = UUID.randomUUID(),
        login = "test",
        displayName = "Test",
        roleCodes = roles.toSet(),
    )

    private fun material(id: UUID = UUID.randomUUID()) = Material(
        id = id,
        name = "Material",
        unit = MeasurementUnit.PIECE,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Test
    fun `blocks deletion when material is referenced in active BOM`() {
        val id = UUID.randomUUID()
        val cmd = DeleteMaterialCommand(id, actor("WAREHOUSE"))
        every { materialPort.findById(id) } returns material(id)
        every { materialPort.hasMovements(id) } returns false
        every { requirementPort.existsInActiveOrder(id) } returns true

        val result = useCase.delete(cmd)
        assertInstanceOf(InventoryMutationResult.Conflict::class.java, result)
    }

    @Test
    fun `allows deletion when no movements and no active BOM references`() {
        val id = UUID.randomUUID()
        val cmd = DeleteMaterialCommand(id, actor("WAREHOUSE"))
        every { materialPort.findById(id) } returns material(id)
        every { materialPort.hasMovements(id) } returns false
        every { requirementPort.existsInActiveOrder(id) } returns false
        every { requirementPort.deleteByMaterialIdInShippedOrders(id) } returns 1
        every { materialPort.deleteById(id) } returns Unit
        every { auditPort.record(any()) } answers { firstArg() }

        val result = useCase.delete(cmd)
        assertInstanceOf(InventoryMutationResult.Success::class.java, result)
        verify { requirementPort.deleteByMaterialIdInShippedOrders(id) }
    }
}

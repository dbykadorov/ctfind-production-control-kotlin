package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
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

class CreateMaterialUseCaseTests {

    private val materialPort = mockk<MaterialPort>()
    private val auditPort = mockk<InventoryAuditPort>()
    private val useCase = CreateMaterialUseCase(materialPort, auditPort)

    private fun actor(vararg roles: String) = AuthenticatedInventoryActor(
        userId = UUID.randomUUID(),
        login = "test",
        displayName = "Test User",
        roleCodes = roles.toSet(),
    )

    @Test
    fun `returns Forbidden when actor lacks WAREHOUSE role`() {
        val cmd = CreateMaterialCommand("Steel", MeasurementUnit.PIECE, actor("ORDER_MANAGER"))
        val result = useCase.create(cmd)
        assertInstanceOf(InventoryMutationResult.Forbidden::class.java, result)
    }

    @Test
    fun `returns ValidationFailed when name is blank`() {
        val cmd = CreateMaterialCommand("  ", MeasurementUnit.PIECE, actor("WAREHOUSE"))
        every { materialPort.existsByNameIgnoreCase(any()) } returns false
        val result = useCase.create(cmd)
        assertInstanceOf(InventoryMutationResult.ValidationFailed::class.java, result)
    }

    @Test
    fun `returns Conflict when name already exists`() {
        val cmd = CreateMaterialCommand("Steel", MeasurementUnit.PIECE, actor("WAREHOUSE"))
        every { materialPort.existsByNameIgnoreCase("Steel") } returns true
        val result = useCase.create(cmd)
        assertInstanceOf(InventoryMutationResult.Conflict::class.java, result)
    }

    @Test
    fun `returns Success and records audit when valid`() {
        val cmd = CreateMaterialCommand("Steel", MeasurementUnit.PIECE, actor("WAREHOUSE"))
        val now = Instant.now()
        val saved = Material(id = UUID.randomUUID(), name = "Steel", unit = MeasurementUnit.PIECE, createdAt = now, updatedAt = now)

        every { materialPort.existsByNameIgnoreCase("Steel") } returns false
        every { materialPort.save(any()) } returns saved
        val eventSlot = slot<InventoryAuditEvent>()
        every { auditPort.record(capture(eventSlot)) } answers { eventSlot.captured }

        val result = useCase.create(cmd)

        assertInstanceOf(InventoryMutationResult.Success::class.java, result)
        assertEquals("Steel", (result as InventoryMutationResult.Success).value.name)
        verify { auditPort.record(match { it.eventType == "MATERIAL_CREATED" }) }
    }

    @Test
    fun `Admin role is also allowed`() {
        val cmd = CreateMaterialCommand("Steel", MeasurementUnit.PIECE, actor("ADMIN"))
        val now = Instant.now()
        val saved = Material(id = UUID.randomUUID(), name = "Steel", unit = MeasurementUnit.PIECE, createdAt = now, updatedAt = now)

        every { materialPort.existsByNameIgnoreCase("Steel") } returns false
        every { materialPort.save(any()) } returns saved
        every { auditPort.record(any()) } answers { firstArg() }

        val result = useCase.create(cmd)
        assertInstanceOf(InventoryMutationResult.Success::class.java, result)
    }
}

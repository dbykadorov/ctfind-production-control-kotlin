package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import com.ctfind.productioncontrol.inventory.domain.OrderMaterialRequirement
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class UpdateBomLineUseCaseTest {
    private val materials = mockk<MaterialPort>()
    private val requirements = mockk<OrderMaterialRequirementPort>()
    private val orders = mockk<OrderLookupPort>()
    private val audit = mockk<InventoryAuditPort>()
    private val useCase = UpdateBomLineUseCase(materials, requirements, orders, audit)

    private fun actor(vararg roles: String) = AuthenticatedInventoryActor(
        userId = UUID.randomUUID(),
        login = "tester",
        displayName = "Tester",
        roleCodes = roles.toSet(),
    )

    private fun command(
        orderId: UUID = UUID.randomUUID(),
        lineId: UUID = UUID.randomUUID(),
        quantity: BigDecimal = BigDecimal("12"),
        roles: Set<String> = setOf("ORDER_MANAGER"),
    ) = UpdateBomLineCommand(
        orderId = orderId,
        lineId = lineId,
        quantity = quantity,
        comment = "Обновлено",
        actor = actor(*roles.toTypedArray()),
    )

    private fun existingLine(orderId: UUID, lineId: UUID, materialId: UUID) = OrderMaterialRequirement(
        id = lineId,
        orderId = orderId,
        materialId = materialId,
        quantity = BigDecimal("10"),
        comment = "Исходно",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Test
    fun `forbidden without ORDER_MANAGER role`() {
        val result = useCase.update(command(roles = setOf("WAREHOUSE")))
        assertInstanceOf(InventoryMutationResult.Forbidden::class.java, result)
    }

    @Test
    fun `returns BomLineNotFound when line missing`() {
        val cmd = command()
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )
        every { requirements.findByLineIdAndOrderId(cmd.lineId, cmd.orderId) } returns null
        val result = useCase.update(cmd)
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
        val result = useCase.update(cmd)
        assertInstanceOf(InventoryMutationResult.OrderLocked::class.java, result)
    }

    @Test
    fun `returns ValidationFailed when quantity is not positive`() {
        val result = useCase.update(command(quantity = BigDecimal.ZERO))
        assertInstanceOf(InventoryMutationResult.ValidationFailed::class.java, result)
    }

    @Test
    fun `success records BOM_LINE_UPDATED audit`() {
        val cmd = command()
        val materialId = UUID.randomUUID()
        val existing = existingLine(cmd.orderId, cmd.lineId, materialId)
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )
        every { requirements.findByLineIdAndOrderId(cmd.lineId, cmd.orderId) } returns existing
        every { materials.findById(materialId) } returns Material(
            id = materialId,
            name = "Фанера",
            unit = MeasurementUnit.SQUARE_METER,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        every { requirements.save(any()) } answers { firstArg() }
        val eventSlot = slot<InventoryAuditEvent>()
        every { audit.record(capture(eventSlot)) } answers { eventSlot.captured }

        val result = useCase.update(cmd)

        assertInstanceOf(InventoryMutationResult.Success::class.java, result)
        verify { audit.record(match { it.eventType == "BOM_LINE_UPDATED" }) }
    }
}

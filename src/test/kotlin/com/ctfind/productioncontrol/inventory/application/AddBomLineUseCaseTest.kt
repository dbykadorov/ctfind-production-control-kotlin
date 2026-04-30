package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
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

class AddBomLineUseCaseTest {
    private val materials = mockk<MaterialPort>()
    private val requirements = mockk<OrderMaterialRequirementPort>()
    private val orders = mockk<OrderLookupPort>()
    private val audit = mockk<InventoryAuditPort>()
    private val useCase = AddBomLineUseCase(materials, requirements, orders, audit)

    private fun actor(vararg roles: String) = AuthenticatedInventoryActor(
        userId = UUID.randomUUID(),
        login = "tester",
        displayName = "Tester",
        roleCodes = roles.toSet(),
    )

    private fun command(
        orderId: UUID = UUID.randomUUID(),
        materialId: UUID = UUID.randomUUID(),
        quantity: BigDecimal = BigDecimal("10"),
        roles: Set<String> = setOf("ORDER_MANAGER"),
    ) = AddBomLineCommand(
        orderId = orderId,
        materialId = materialId,
        quantity = quantity,
        comment = "На основу",
        actor = actor(*roles.toTypedArray()),
    )

    @Test
    fun `forbidden without ORDER_MANAGER role`() {
        val result = useCase.add(command(roles = setOf("WAREHOUSE")))
        assertInstanceOf(InventoryMutationResult.Forbidden::class.java, result)
    }

    @Test
    fun `returns OrderNotFound when order does not exist`() {
        val cmd = command()
        every { orders.findOrderSummary(cmd.orderId) } returns null
        val result = useCase.add(cmd)
        assertInstanceOf(InventoryMutationResult.OrderNotFound::class.java, result)
    }

    @Test
    fun `returns OrderLocked when order is shipped`() {
        val cmd = command()
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.SHIPPED,
        )
        val result = useCase.add(cmd)
        assertInstanceOf(InventoryMutationResult.OrderLocked::class.java, result)
    }

    @Test
    fun `returns MaterialNotFound when material does not exist`() {
        val cmd = command()
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )
        every { materials.findById(cmd.materialId) } returns null
        val result = useCase.add(cmd)
        assertInstanceOf(InventoryMutationResult.MaterialNotFound::class.java, result)
    }

    @Test
    fun `returns Conflict when duplicate BOM line exists`() {
        val cmd = command()
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )
        every { materials.findById(cmd.materialId) } returns Material(
            id = cmd.materialId,
            name = "Фанера",
            unit = MeasurementUnit.SQUARE_METER,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        every { requirements.existsByOrderIdAndMaterialId(cmd.orderId, cmd.materialId) } returns true
        val result = useCase.add(cmd)
        assertInstanceOf(InventoryMutationResult.Conflict::class.java, result)
    }

    @Test
    fun `returns ValidationFailed when quantity is not positive`() {
        val result = useCase.add(command(quantity = BigDecimal.ZERO))
        assertInstanceOf(InventoryMutationResult.ValidationFailed::class.java, result)
    }

    @Test
    fun `success records audit event`() {
        val cmd = command()
        val material = Material(
            id = cmd.materialId,
            name = "Фанера",
            unit = MeasurementUnit.SQUARE_METER,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        every { orders.findOrderSummary(cmd.orderId) } returns InventoryOrderSummary(
            id = cmd.orderId,
            orderNumber = "ORD-1",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )
        every { materials.findById(cmd.materialId) } returns material
        every { requirements.existsByOrderIdAndMaterialId(cmd.orderId, cmd.materialId) } returns false
        every { requirements.save(any()) } answers { firstArg() }
        val eventSlot = slot<InventoryAuditEvent>()
        every { audit.record(capture(eventSlot)) } answers { eventSlot.captured }

        val result = useCase.add(cmd)

        assertInstanceOf(InventoryMutationResult.Success::class.java, result)
        verify { audit.record(match { it.eventType == "BOM_LINE_ADDED" }) }
    }
}

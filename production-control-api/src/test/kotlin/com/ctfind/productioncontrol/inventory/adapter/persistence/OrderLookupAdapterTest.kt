package com.ctfind.productioncontrol.inventory.adapter.persistence

import com.ctfind.productioncontrol.orders.adapter.persistence.CustomerEntity
import com.ctfind.productioncontrol.orders.adapter.persistence.CustomerJpaRepository
import com.ctfind.productioncontrol.orders.adapter.persistence.CustomerOrderEntity
import com.ctfind.productioncontrol.orders.adapter.persistence.CustomerOrderJpaRepository
import com.ctfind.productioncontrol.orders.domain.CustomerStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class OrderLookupAdapterTest {
    private val orderRepo = mockk<CustomerOrderJpaRepository>()
    private val customerRepo = mockk<CustomerJpaRepository>()
    private val requirementRepo = mockk<OrderMaterialRequirementJpaRepository>()
    private val adapter = OrderLookupAdapter(orderRepo, customerRepo, requirementRepo)

    @Test
    fun `findOrderSummary returns null for unknown order`() {
        val orderId = UUID.randomUUID()
        every { orderRepo.findById(orderId) } returns Optional.empty()
        assertNull(adapter.findOrderSummary(orderId))
    }

    @Test
    fun `searchActiveOrdersForConsumption returns only active orders with BOM`() {
        val customer = customer(UUID.randomUUID(), "Acme")
        val orderWithBom = order(
            id = UUID.randomUUID(),
            number = "ORD-001",
            customer = customer,
            status = OrderStatus.IN_WORK,
            createdAt = Instant.parse("2026-04-29T10:00:00Z"),
        )
        val shippedOrder = order(
            id = UUID.randomUUID(),
            number = "ORD-002",
            customer = customer,
            status = OrderStatus.SHIPPED,
            createdAt = Instant.parse("2026-04-30T10:00:00Z"),
        )
        every { requirementRepo.findDistinctOrderIds() } returns listOf(orderWithBom.id, shippedOrder.id)
        every { orderRepo.findAllById(any<Iterable<UUID>>()) } returns listOf(orderWithBom, shippedOrder)
        every { customerRepo.findById(customer.id) } returns Optional.of(customer)

        val result = adapter.searchActiveOrdersForConsumption(
            com.ctfind.productioncontrol.inventory.application.ActiveOrderSearchQuery(search = "ORD", limit = 20),
        )

        assertEquals(1, result.size)
        assertEquals(orderWithBom.id, result.first().id)
    }

    private fun customer(id: UUID, name: String) = CustomerEntity(
        id = id,
        displayName = name,
        status = CustomerStatus.ACTIVE,
        contactPerson = null,
        phone = null,
        email = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private fun order(
        id: UUID,
        number: String,
        customer: CustomerEntity,
        status: OrderStatus,
        createdAt: Instant,
    ) = CustomerOrderEntity(
        id = id,
        orderNumber = number,
        customer = customer,
        deliveryDate = LocalDate.parse("2026-05-10"),
        status = status,
        notes = null,
        createdByUserId = UUID.randomUUID(),
        createdAt = createdAt,
        updatedAt = createdAt,
        version = 0,
        items = mutableListOf(),
    )
}

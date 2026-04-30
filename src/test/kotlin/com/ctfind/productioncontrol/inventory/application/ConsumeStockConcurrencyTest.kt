package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import com.ctfind.productioncontrol.inventory.domain.MovementType
import com.ctfind.productioncontrol.inventory.domain.OrderMaterialRequirement
import com.ctfind.productioncontrol.inventory.domain.StockMovement
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

class ConsumeStockConcurrencyTest {

    private val actor = AuthenticatedInventoryActor(
        userId = UUID.fromString("20000000-0000-0000-0000-000000000002"),
        login = "warehouse",
        displayName = "Warehouse User",
        roleCodes = setOf("WAREHOUSE"),
    )

    @Test
    fun `two concurrent consume requests keep stock non-negative`() {
        val materialId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val lock = ReentrantLock()
        val stock = AtomicReference(BigDecimal("50"))
        val material = Material(
            id = materialId,
            name = "Фанера",
            unit = MeasurementUnit.SQUARE_METER,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val order = InventoryOrderSummary(
            id = orderId,
            orderNumber = "ORD-42",
            customerName = "Acme",
            status = OrderStatus.IN_WORK,
        )

        val useCase = ConsumeStockUseCase(
            materials = InMemoryMaterialPort(material, lock),
            requirements = InMemoryRequirementPort(orderId, materialId),
            movements = InMemoryStockMovementPort(stock),
            orders = InMemoryOrderLookupPort(order),
            audit = InMemoryAuditPort(),
        )

        val results = ConcurrentLinkedQueue<InventoryMutationResult<StockMovementView>>()
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val done = CountDownLatch(2)

        repeat(2) {
            thread(name = "consume-$it") {
                ready.countDown()
                start.await()
                val result = try {
                    useCase.consume(
                        ConsumeStockCommand(
                            materialId = materialId,
                            orderId = orderId,
                            quantity = BigDecimal("30"),
                            comment = "Concurrent consume",
                            actor = actor,
                        ),
                    )
                } finally {
                    if (lock.isHeldByCurrentThread) {
                        lock.unlock()
                    }
                }
                results += result
                done.countDown()
            }
        }

        assertTrue(ready.await(2, TimeUnit.SECONDS), "Threads did not get ready in time")
        start.countDown()
        assertTrue(done.await(5, TimeUnit.SECONDS), "Threads did not finish in time")

        val successCount = results.count { it is InventoryMutationResult.Success }
        val insufficientCount = results.count { it is InventoryMutationResult.InsufficientStock }

        assertEquals(1, successCount, "Exactly one request must succeed")
        assertEquals(1, insufficientCount, "Exactly one request must fail with insufficient stock")

        val finalStock = stock.get()
        assertTrue(
            finalStock == BigDecimal("20") || finalStock == BigDecimal("50"),
            "Final stock must be 20 or 50 depending on execution order",
        )
        assertTrue(finalStock >= BigDecimal.ZERO, "Final stock must never be negative")
    }

    private class InMemoryMaterialPort(
        private val material: Material,
        private val lock: ReentrantLock,
    ) : MaterialPort {
        override fun findById(id: UUID): Material? = if (id == material.id) material else null
        override fun findByIdForUpdate(id: UUID): Material? {
            if (id != material.id) return null
            lock.lock()
            return material
        }

        override fun findAll(query: MaterialListQuery): List<Material> = error("Not used in test")
        override fun count(query: MaterialListQuery): Long = error("Not used in test")
        override fun save(material: Material): Material = error("Not used in test")
        override fun deleteById(id: UUID) = error("Not used in test")
        override fun existsByNameIgnoreCase(name: String): Boolean = error("Not used in test")
        override fun existsByNameIgnoreCaseAndIdNot(name: String, id: UUID): Boolean = error("Not used in test")
        override fun hasMovements(materialId: UUID): Boolean = error("Not used in test")
    }

    private class InMemoryRequirementPort(
        private val orderId: UUID,
        private val materialId: UUID,
    ) : OrderMaterialRequirementPort {
        override fun save(requirement: OrderMaterialRequirement): OrderMaterialRequirement = error("Not used in test")
        override fun findByLineId(id: UUID): OrderMaterialRequirement? = error("Not used in test")
        override fun findByLineIdAndOrderId(id: UUID, orderId: UUID): OrderMaterialRequirement? = error("Not used in test")
        override fun findByOrderIdOrderByCreatedAtDesc(orderId: UUID): List<OrderMaterialRequirement> = error("Not used in test")
        override fun findByOrderIdAndMaterialId(orderId: UUID, materialId: UUID): OrderMaterialRequirement? = error("Not used in test")
        override fun existsByOrderIdAndMaterialId(orderId: UUID, materialId: UUID): Boolean =
            this.orderId == orderId && this.materialId == materialId

        override fun existsInActiveOrder(materialId: UUID): Boolean = error("Not used in test")
        override fun deleteByMaterialIdInShippedOrders(materialId: UUID): Int = error("Not used in test")
        override fun deleteLineById(id: UUID) = error("Not used in test")
    }

    private class InMemoryStockMovementPort(
        private val stock: AtomicReference<BigDecimal>,
    ) : StockMovementPort {
        override fun save(movement: StockMovement): StockMovement {
            val updated = stock.updateAndGet { current -> current.subtract(movement.quantity) }
            check(updated >= BigDecimal.ZERO) { "Stock became negative: $updated" }
            return movement
        }

        override fun findByMaterialId(materialId: UUID, page: Int, size: Int): List<StockMovement> = error("Not used in test")
        override fun countByMaterialId(materialId: UUID): Long = error("Not used in test")
        override fun sumQuantityByMaterialId(materialId: UUID): BigDecimal = stock.get()
        override fun sumQuantityByMaterialIdAndType(materialId: UUID, movementType: MovementType): BigDecimal = error("Not used in test")
        override fun hasConsumption(orderId: UUID, materialId: UUID): Boolean = error("Not used in test")
        override fun sumConsumedQuantity(orderId: UUID, materialId: UUID): BigDecimal = error("Not used in test")
        override fun sumConsumedByOrder(orderId: UUID): Map<UUID, BigDecimal> = error("Not used in test")
    }

    private class InMemoryOrderLookupPort(
        private val order: InventoryOrderSummary,
    ) : OrderLookupPort {
        override fun findOrderSummary(orderId: UUID): InventoryOrderSummary? = if (order.id == orderId) order else null
        override fun searchActiveOrdersForConsumption(query: ActiveOrderSearchQuery): List<InventoryOrderSummary> = error("Not used in test")
    }

    private class InMemoryAuditPort : InventoryAuditPort {
        override fun record(event: InventoryAuditEvent): InventoryAuditEvent = event
    }
}

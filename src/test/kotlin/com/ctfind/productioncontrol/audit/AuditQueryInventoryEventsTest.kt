package com.ctfind.productioncontrol.audit

import com.ctfind.productioncontrol.audit.application.AuditLogPageResult
import com.ctfind.productioncontrol.audit.application.AuditLogQuery
import com.ctfind.productioncontrol.audit.application.AuditLogQueryPort
import com.ctfind.productioncontrol.audit.application.AuditLogQueryResult
import com.ctfind.productioncontrol.audit.application.AuditLogQueryUseCase
import com.ctfind.productioncontrol.audit.domain.AuditCategory
import com.ctfind.productioncontrol.audit.domain.AuditLogRow
import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.inventory.application.ActiveOrderSearchQuery
import com.ctfind.productioncontrol.inventory.application.AddBomLineCommand
import com.ctfind.productioncontrol.inventory.application.AddBomLineUseCase
import com.ctfind.productioncontrol.inventory.application.AuthenticatedInventoryActor
import com.ctfind.productioncontrol.inventory.application.ConsumeStockCommand
import com.ctfind.productioncontrol.inventory.application.ConsumeStockUseCase
import com.ctfind.productioncontrol.inventory.application.InventoryAuditPort
import com.ctfind.productioncontrol.inventory.application.InventoryMutationResult
import com.ctfind.productioncontrol.inventory.application.InventoryOrderSummary
import com.ctfind.productioncontrol.inventory.application.MaterialListQuery
import com.ctfind.productioncontrol.inventory.application.MaterialPort
import com.ctfind.productioncontrol.inventory.application.OrderLookupPort
import com.ctfind.productioncontrol.inventory.application.OrderMaterialRequirementPort
import com.ctfind.productioncontrol.inventory.application.RemoveBomLineCommand
import com.ctfind.productioncontrol.inventory.application.RemoveBomLineUseCase
import com.ctfind.productioncontrol.inventory.application.StockMovementPort
import com.ctfind.productioncontrol.inventory.application.StockMovementView
import com.ctfind.productioncontrol.inventory.application.UpdateBomLineCommand
import com.ctfind.productioncontrol.inventory.application.UpdateBomLineUseCase
import com.ctfind.productioncontrol.inventory.domain.InventoryAuditEvent
import com.ctfind.productioncontrol.inventory.domain.Material
import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import com.ctfind.productioncontrol.inventory.domain.MovementType
import com.ctfind.productioncontrol.inventory.domain.OrderMaterialRequirement
import com.ctfind.productioncontrol.inventory.domain.StockMovement
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class AuditQueryInventoryEventsTest {

    @Test
    fun `audit query returns four inventory event types in descending order`() {
        val materialId = UUID.randomUUID()
        val orderForBom = UUID.randomUUID()
        val orderForConsume = UUID.randomUUID()
        val material = Material(
            id = materialId,
            name = "Фанера",
            unit = MeasurementUnit.SQUARE_METER,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val now = Instant.now()
        val seedLineForConsume = OrderMaterialRequirement(
            id = UUID.randomUUID(),
            orderId = orderForConsume,
            materialId = materialId,
            quantity = BigDecimal("20"),
            comment = null,
            createdAt = now,
            updatedAt = now,
        )

        val materials = InMemoryMaterialPort(material)
        val requirements = InMemoryRequirementPort(seedLineForConsume)
        val movements = InMemoryStockMovementPort(BigDecimal("100"))
        val orders = InMemoryOrderLookupPort(
            orderSummaries = mapOf(
                orderForBom to orderSummary(orderForBom, "ORD-10"),
                orderForConsume to orderSummary(orderForConsume, "ORD-11"),
            ),
        )
        val auditSink = InMemoryInventoryAuditPort()

        val addBomLineUseCase = AddBomLineUseCase(materials, requirements, orders, auditSink)
        val updateBomLineUseCase = UpdateBomLineUseCase(materials, requirements, orders, auditSink)
        val removeBomLineUseCase = RemoveBomLineUseCase(materials, requirements, movements, orders, auditSink)
        val consumeStockUseCase = ConsumeStockUseCase(materials, requirements, movements, orders, auditSink)

        val orderManager = actor("order-manager", "ORDER_MANAGER")
        val warehouse = actor("warehouse", "WAREHOUSE")

        val addResult = addBomLineUseCase.add(
            AddBomLineCommand(
                orderId = orderForBom,
                materialId = materialId,
                quantity = BigDecimal("10"),
                comment = "Первичная строка",
                actor = orderManager,
            ),
        )
        val addedLine = when (addResult) {
            is InventoryMutationResult.Success -> addResult.value
            else -> error("Expected successful BOM add, got $addResult")
        }

        Thread.sleep(2)
        val updateResult = updateBomLineUseCase.update(
            UpdateBomLineCommand(
                orderId = orderForBom,
                lineId = addedLine.id,
                quantity = BigDecimal("12"),
                comment = "Уточнили количество",
                actor = orderManager,
            ),
        )
        assertInstanceOf(InventoryMutationResult.Success::class.java, updateResult)

        Thread.sleep(2)
        val removeResult = removeBomLineUseCase.remove(
            RemoveBomLineCommand(
                orderId = orderForBom,
                lineId = addedLine.id,
                actor = orderManager,
            ),
        )
        assertInstanceOf(InventoryMutationResult.Success::class.java, removeResult)

        Thread.sleep(2)
        val consumeResult = consumeStockUseCase.consume(
            ConsumeStockCommand(
                materialId = materialId,
                orderId = orderForConsume,
                quantity = BigDecimal("7"),
                comment = "На производство",
                actor = warehouse,
            ),
        )
        assertInstanceOf(InventoryMutationResult.Success::class.java, consumeResult)

        val queryUseCase = AuditLogQueryUseCase(
            auditLog = InMemoryAuditLogQueryPort(auditSink.events),
        )
        val queryResult = queryUseCase.list(
            query = AuditLogQuery(
                from = Instant.EPOCH,
                to = Instant.now().plusSeconds(5),
                categories = setOf(AuditCategory.INVENTORY),
                page = 0,
                size = 20,
            ),
            roleCodes = setOf(ADMIN_ROLE_CODE),
        )

        val page = assertInstanceOf(AuditLogQueryResult.Success::class.java, queryResult).page
        assertEquals(4, page.items.size)
        assertEquals(
            listOf("STOCK_CONSUMPTION", "BOM_LINE_REMOVED", "BOM_LINE_UPDATED", "BOM_LINE_ADDED"),
            page.items.map { it.eventType },
        )
    }

    private fun actor(login: String, vararg roles: String) = AuthenticatedInventoryActor(
        userId = UUID.randomUUID(),
        login = login,
        displayName = login,
        roleCodes = roles.toSet(),
    )

    private fun orderSummary(orderId: UUID, number: String) = InventoryOrderSummary(
        id = orderId,
        orderNumber = number,
        customerName = "Acme",
        status = OrderStatus.IN_WORK,
    )

    private class InMemoryMaterialPort(
        private val material: Material,
    ) : MaterialPort {
        override fun findById(id: UUID): Material? = if (material.id == id) material else null
        override fun findByIdForUpdate(id: UUID): Material? = findById(id)
        override fun findAll(query: MaterialListQuery): List<Material> = error("Not used in test")
        override fun count(query: MaterialListQuery): Long = error("Not used in test")
        override fun save(material: Material): Material = error("Not used in test")
        override fun deleteById(id: UUID) = error("Not used in test")
        override fun existsByNameIgnoreCase(name: String): Boolean = error("Not used in test")
        override fun existsByNameIgnoreCaseAndIdNot(name: String, id: UUID): Boolean = error("Not used in test")
        override fun hasMovements(materialId: UUID): Boolean = error("Not used in test")
    }

    private class InMemoryRequirementPort(
        seedRequirement: OrderMaterialRequirement,
    ) : OrderMaterialRequirementPort {
        private val byLineId = ConcurrentHashMap<UUID, OrderMaterialRequirement>()

        init {
            byLineId[seedRequirement.id] = seedRequirement
        }

        override fun save(requirement: OrderMaterialRequirement): OrderMaterialRequirement {
            byLineId[requirement.id] = requirement
            return requirement
        }

        override fun findByLineId(id: UUID): OrderMaterialRequirement? = byLineId[id]

        override fun findByLineIdAndOrderId(id: UUID, orderId: UUID): OrderMaterialRequirement? =
            byLineId[id]?.takeIf { it.orderId == orderId }

        override fun findByOrderIdOrderByCreatedAtDesc(orderId: UUID): List<OrderMaterialRequirement> =
            byLineId.values.filter { it.orderId == orderId }.sortedByDescending { it.createdAt }

        override fun findByOrderIdAndMaterialId(orderId: UUID, materialId: UUID): OrderMaterialRequirement? =
            byLineId.values.firstOrNull { it.orderId == orderId && it.materialId == materialId }

        override fun existsByOrderIdAndMaterialId(orderId: UUID, materialId: UUID): Boolean =
            findByOrderIdAndMaterialId(orderId, materialId) != null

        override fun existsInActiveOrder(materialId: UUID): Boolean =
            byLineId.values.any { it.materialId == materialId }

        override fun deleteByMaterialIdInShippedOrders(materialId: UUID): Int = error("Not used in test")

        override fun deleteLineById(id: UUID) {
            byLineId.remove(id)
        }
    }

    private class InMemoryStockMovementPort(
        private val receiptTotal: BigDecimal,
    ) : StockMovementPort {
        private val movements = mutableListOf<StockMovement>()

        override fun save(movement: StockMovement): StockMovement {
            movements += movement
            return movement
        }

        override fun findByMaterialId(materialId: UUID, page: Int, size: Int): List<StockMovement> = error("Not used in test")
        override fun countByMaterialId(materialId: UUID): Long = error("Not used in test")

        override fun sumQuantityByMaterialId(materialId: UUID): BigDecimal {
            val consumed = movements
                .filter { it.materialId == materialId && it.movementType == MovementType.CONSUMPTION }
                .fold(BigDecimal.ZERO) { acc, movement -> acc + movement.quantity }
            return receiptTotal - consumed
        }

        override fun sumQuantityByMaterialIdAndType(materialId: UUID, movementType: MovementType): BigDecimal = error("Not used in test")

        override fun hasConsumption(orderId: UUID, materialId: UUID): Boolean =
            movements.any {
                it.orderId == orderId &&
                    it.materialId == materialId &&
                    it.movementType == MovementType.CONSUMPTION
            }

        override fun sumConsumedQuantity(orderId: UUID, materialId: UUID): BigDecimal = error("Not used in test")
        override fun sumConsumedByOrder(orderId: UUID): Map<UUID, BigDecimal> = error("Not used in test")
    }

    private class InMemoryOrderLookupPort(
        private val orderSummaries: Map<UUID, InventoryOrderSummary>,
    ) : OrderLookupPort {
        override fun findOrderSummary(orderId: UUID): InventoryOrderSummary? = orderSummaries[orderId]

        override fun searchActiveOrdersForConsumption(query: ActiveOrderSearchQuery): List<InventoryOrderSummary> =
            orderSummaries.values.take(query.limit)
    }

    private class InMemoryInventoryAuditPort : InventoryAuditPort {
        val events = mutableListOf<InventoryAuditEvent>()

        override fun record(event: InventoryAuditEvent): InventoryAuditEvent {
            events += event
            return event
        }
    }

    private class InMemoryAuditLogQueryPort(
        private val inventoryEvents: List<InventoryAuditEvent>,
    ) : AuditLogQueryPort {
        override fun search(query: AuditLogQuery): AuditLogPageResult {
            val includeInventory = query.categories?.contains(AuditCategory.INVENTORY) ?: true
            val filtered = if (includeInventory) {
                inventoryEvents.filter { it.eventAt >= query.from && it.eventAt < query.to }
            } else {
                emptyList()
            }

            val rows = filtered
                .map { event ->
                    AuditLogRow(
                        id = event.id,
                        occurredAt = event.eventAt,
                        category = AuditCategory.INVENTORY,
                        eventType = event.eventType,
                        actorUserId = event.actorUserId,
                        actorDisplayName = event.actorUserId.toString(),
                        actorLogin = null,
                        summary = event.summary,
                        targetType = "MATERIAL",
                        targetId = event.targetId,
                    )
                }
                .sortedByDescending { it.occurredAt }

            val fromIndex = (query.page * query.size).coerceAtMost(rows.size)
            val toIndex = min(fromIndex + query.size, rows.size)
            return AuditLogPageResult(
                items = rows.subList(fromIndex, toIndex),
                page = query.page,
                size = query.size,
                totalItems = rows.size.toLong(),
            )
        }
    }
}

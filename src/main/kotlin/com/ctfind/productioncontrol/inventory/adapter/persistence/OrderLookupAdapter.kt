package com.ctfind.productioncontrol.inventory.adapter.persistence

import com.ctfind.productioncontrol.inventory.application.ActiveOrderSearchQuery
import com.ctfind.productioncontrol.inventory.application.InventoryOrderSummary
import com.ctfind.productioncontrol.inventory.application.OrderLookupPort
import com.ctfind.productioncontrol.orders.adapter.persistence.CustomerJpaRepository
import com.ctfind.productioncontrol.orders.adapter.persistence.CustomerOrderEntity
import com.ctfind.productioncontrol.orders.adapter.persistence.CustomerOrderJpaRepository
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrderLookupAdapter(
    private val orderRepo: CustomerOrderJpaRepository,
    private val customerRepo: CustomerJpaRepository,
    private val requirementRepo: OrderMaterialRequirementJpaRepository,
) : OrderLookupPort {
    override fun findOrderSummary(orderId: UUID): InventoryOrderSummary? {
        val order = orderRepo.findById(orderId).orElse(null) ?: return null
        val customerName = customerRepo.findById(order.customer.id).orElse(order.customer).displayName
        return order.toSummary(customerName)
    }

    override fun searchActiveOrdersForConsumption(query: ActiveOrderSearchQuery): List<InventoryOrderSummary> {
        val orderIds = requirementRepo.findDistinctOrderIds()
        if (orderIds.isEmpty()) return emptyList()

        val normalizedSearch = query.search?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val limit = query.limit.coerceIn(1, 100)

        return orderRepo.findAllById(orderIds)
            .asSequence()
            .filter { it.status != OrderStatus.SHIPPED }
            .map { order ->
                val customerName = customerRepo.findById(order.customer.id).orElse(order.customer).displayName
                Triple(order, customerName, "${order.orderNumber} $customerName".lowercase())
            }
            .filter { (_, _, haystack) -> normalizedSearch == null || haystack.contains(normalizedSearch) }
            .sortedByDescending { (order) -> order.createdAt }
            .take(limit)
            .map { (order, customerName) -> order.toSummary(customerName) }
            .toList()
    }
}

private fun CustomerOrderEntity.toSummary(customerName: String): InventoryOrderSummary =
    InventoryOrderSummary(
        id = id,
        orderNumber = orderNumber,
        customerName = customerName,
        status = status,
    )

package com.ctfind.productioncontrol.inventory.application

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class InventoryQueryUseCase @Autowired constructor(
    private val materials: MaterialPort,
    private val movements: StockMovementPort,
    private val requirements: OrderMaterialRequirementPort,
    private val orderLookup: OrderLookupPort,
) {
    fun listMaterials(query: MaterialListQuery): MaterialsPageResult {
        val items = materials.findAll(query)
        val total = materials.count(query)
        val totalPages = if (query.size > 0) ((total + query.size - 1) / query.size).toInt() else 0
        val views = items.map { material ->
            val stock = movements.sumQuantityByMaterialId(material.id)
            material.toView(stock)
        }
        return MaterialsPageResult(
            items = views,
            page = query.page,
            size = query.size,
            totalItems = total,
            totalPages = totalPages,
        )
    }

    fun getMaterial(id: UUID): MaterialView? {
        val material = materials.findById(id) ?: return null
        val stock = movements.sumQuantityByMaterialId(id)
        return material.toView(stock)
    }

    fun listMovements(materialId: UUID, page: Int, size: Int): StockMovementsPageResult? {
        materials.findById(materialId) ?: return null
        val items = movements.findByMaterialId(materialId, page, size)
        val total = movements.countByMaterialId(materialId)
        val totalPages = if (size > 0) ((total + size - 1) / size).toInt() else 0
        return StockMovementsPageResult(
            items = items.map { it.toView() },
            page = page,
            size = size,
            totalItems = total,
            totalPages = totalPages,
        )
    }

    fun listBom(orderId: UUID): List<BomLineView>? {
        orderLookup.findOrderSummary(orderId) ?: return null
        return requirements.findByOrderIdOrderByCreatedAtDesc(orderId)
            .mapNotNull { line ->
                val material = materials.findById(line.materialId) ?: return@mapNotNull null
                line.toView(material.name, material.unit)
            }
    }

    fun getMaterialUsage(orderId: UUID): MaterialUsageView? {
        orderLookup.findOrderSummary(orderId) ?: return null
        val lines = requirements.findByOrderIdOrderByCreatedAtDesc(orderId)
        val consumedByMaterial = movements.sumConsumedByOrder(orderId)

        val rows = lines.mapNotNull { line ->
            val material = materials.findById(line.materialId) ?: return@mapNotNull null
            val consumed = consumedByMaterial[line.materialId] ?: BigDecimal.ZERO
            val required = line.quantity
            MaterialUsageRowView(
                materialId = line.materialId,
                materialName = material.name,
                materialUnit = material.unit,
                requiredQuantity = required,
                consumedQuantity = consumed,
                remainingToConsume = positiveOrZero(required - consumed),
                overconsumption = positiveOrZero(consumed - required),
            )
        }.sortedBy { it.materialName.lowercase() }

        return MaterialUsageView(orderId = orderId, rows = rows)
    }

    private fun positiveOrZero(value: BigDecimal): BigDecimal =
        if (value < BigDecimal.ZERO) BigDecimal.ZERO else value
}

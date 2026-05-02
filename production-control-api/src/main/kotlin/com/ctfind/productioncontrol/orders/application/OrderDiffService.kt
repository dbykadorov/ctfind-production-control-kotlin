package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.orders.domain.CustomerOrder
import com.ctfind.productioncontrol.orders.domain.OrderFieldDiff

class OrderDiffService {
	fun diff(before: CustomerOrder, after: CustomerOrder): List<OrderFieldDiff> =
		buildList {
			if (before.customerId != after.customerId) {
				add(OrderFieldDiff("customer", "Клиент", before.customerId.toString(), after.customerId.toString()))
			}
			if (before.deliveryDate != after.deliveryDate) {
				add(OrderFieldDiff("delivery_date", "Срок исполнения", before.deliveryDate.toString(), after.deliveryDate.toString()))
			}
			if (before.notes.orEmpty() != after.notes.orEmpty()) {
				add(OrderFieldDiff("notes", "Комментарий", before.notes, after.notes))
			}
			if (before.items.map { it.toComparableValue() } != after.items.map { it.toComparableValue() }) {
				add(OrderFieldDiff("items", "Позиции", serializeItems(before), serializeItems(after)))
			}
		}

	private fun serializeItems(order: CustomerOrder): String =
		order.items.joinToString("; ") { "${it.lineNo}. ${it.itemName} x ${it.quantity} ${it.uom}" }

	private fun com.ctfind.productioncontrol.orders.domain.CustomerOrderItem.toComparableValue(): String =
		listOf(lineNo, itemName.trim(), quantity.stripTrailingZeros().toPlainString(), uom.trim()).joinToString("|")
}

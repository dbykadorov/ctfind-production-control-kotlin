package com.ctfind.productioncontrol.orders

import com.ctfind.productioncontrol.orders.domain.Customer
import com.ctfind.productioncontrol.orders.domain.CustomerOrder
import com.ctfind.productioncontrol.orders.domain.CustomerOrderItem
import com.ctfind.productioncontrol.orders.domain.CustomerStatus
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

object OrderTestFixtures {
	val customerId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
	val orderId: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
	val actorUserId: UUID = UUID.fromString("33333333-3333-3333-3333-333333333333")
	val now: Instant = Instant.parse("2026-04-26T18:00:00Z")

	fun activeCustomer(
		id: UUID = customerId,
		displayName: String = "ООО Ромашка",
	): Customer =
		Customer(
			id = id,
			displayName = displayName,
			status = CustomerStatus.ACTIVE,
			contactPerson = "Иван Иванов",
			phone = "+7 999 000-00-00",
			email = "order@example.test",
			createdAt = now,
			updatedAt = now,
		)

	fun orderItem(
		id: UUID = UUID.fromString("44444444-4444-4444-4444-444444444444"),
		lineNo: Int = 1,
		itemName: String = "Столешница",
		quantity: BigDecimal = BigDecimal("2.0"),
		uom: String = "шт",
	): CustomerOrderItem =
		CustomerOrderItem(
			id = id,
			lineNo = lineNo,
			itemName = itemName,
			quantity = quantity,
			uom = uom,
		)

	fun order(
		id: UUID = orderId,
		status: OrderStatus = OrderStatus.NEW,
		version: Long = 0,
	): CustomerOrder =
		CustomerOrder(
			id = id,
			orderNumber = "ORD-000001",
			customerId = customerId,
			deliveryDate = LocalDate.parse("2026-05-15"),
			status = status,
			notes = "Test order",
			items = listOf(orderItem()),
			createdByUserId = actorUserId,
			createdAt = now,
			updatedAt = now,
			version = version,
		)
}

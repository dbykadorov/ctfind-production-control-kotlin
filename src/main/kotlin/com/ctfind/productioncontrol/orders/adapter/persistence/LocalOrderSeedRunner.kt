package com.ctfind.productioncontrol.orders.adapter.persistence

import com.ctfind.productioncontrol.auth.adapter.persistence.RoleEntity
import com.ctfind.productioncontrol.auth.adapter.persistence.RoleJpaRepository
import com.ctfind.productioncontrol.auth.adapter.persistence.UserAccountEntity
import com.ctfind.productioncontrol.auth.adapter.persistence.UserAccountJpaRepository
import com.ctfind.productioncontrol.auth.application.LocalAdminSeedUseCase
import com.ctfind.productioncontrol.orders.application.ORDER_MANAGER_ROLE_CODE
import com.ctfind.productioncontrol.orders.application.OrderNumberPort
import com.ctfind.productioncontrol.orders.domain.CustomerStatus
import com.ctfind.productioncontrol.orders.domain.OrderChangeType
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Component
@Profile("local")
class LocalOrderSeedRunner(
	private val localAdminSeedUseCase: LocalAdminSeedUseCase,
	private val roleRepository: RoleJpaRepository,
	private val userRepository: UserAccountJpaRepository,
	private val passwordEncoder: PasswordEncoder,
	private val customerRepository: CustomerJpaRepository,
	private val orderRepository: CustomerOrderJpaRepository,
	private val statusChangeRepository: OrderStatusChangeJpaRepository,
	private val changeDiffRepository: OrderChangeDiffJpaRepository,
	private val auditRepository: OrderAuditEventJpaRepository,
	private val orderNumberPort: OrderNumberPort,
) : ApplicationRunner {

	@Transactional
	override fun run(args: ApplicationArguments) {
		localAdminSeedUseCase.seedLocalAdmin()
		val now = Instant.parse("2026-04-26T18:00:00Z")
		val orderManagerRole = roleRepository.findByCode(ORDER_MANAGER_ROLE_CODE)
			?: roleRepository.save(
				RoleEntity(
					id = UUID.randomUUID(),
					code = ORDER_MANAGER_ROLE_CODE,
					name = "Order Manager",
					createdAt = now,
				),
			)

		val manager = userRepository.findByLogin("order.manager")
			?: userRepository.save(
				UserAccountEntity(
					id = UUID.randomUUID(),
					login = "order.manager",
					displayName = "Order Manager",
					passwordHash = passwordEncoder.encode("manager") ?: error("PasswordEncoder returned null hash"),
					enabled = true,
					createdAt = now,
					updatedAt = now,
					roles = mutableSetOf(orderManagerRole),
				),
			)
		if (manager.roles.none { it.code == ORDER_MANAGER_ROLE_CODE }) {
			manager.roles.add(orderManagerRole)
			userRepository.save(manager)
		}

		val admin = userRepository.findByLogin("admin") ?: manager
		val customers = seedCustomers(now)
		if (orderRepository.count() == 0L)
			seedOrders(customers, admin.id, now)
	}

	private fun seedCustomers(now: Instant): List<CustomerEntity> {
		if (customerRepository.count() > 0)
			return customerRepository.findAll()

		return customerRepository.saveAll(
			listOf(
				CustomerEntity(
					id = UUID.randomUUID(),
					displayName = "ООО Ромашка",
					status = CustomerStatus.ACTIVE,
					contactPerson = "Иван Иванов",
					phone = "+7 999 000-00-00",
					email = "orders@romashka.example",
					createdAt = now,
					updatedAt = now,
				),
				CustomerEntity(
					id = UUID.randomUUID(),
					displayName = "ИП Кузнецов",
					status = CustomerStatus.ACTIVE,
					contactPerson = "Петр Кузнецов",
					phone = "+7 999 111-11-11",
					email = "kuznetsov@example.test",
					createdAt = now,
					updatedAt = now,
				),
				CustomerEntity(
					id = UUID.randomUUID(),
					displayName = "Архивный клиент",
					status = CustomerStatus.INACTIVE,
					createdAt = now,
					updatedAt = now,
				),
			),
		)
	}

	private fun seedOrders(customers: List<CustomerEntity>, actorUserId: UUID, now: Instant) {
		val activeCustomers = customers.filter { it.status == CustomerStatus.ACTIVE }
		val statuses = listOf(OrderStatus.NEW, OrderStatus.IN_WORK, OrderStatus.READY, OrderStatus.SHIPPED)
		statuses.forEachIndexed { index, status ->
			val order = CustomerOrderEntity(
				id = UUID.randomUUID(),
				orderNumber = orderNumberPort.nextOrderNumber(),
				customer = activeCustomers[index % activeCustomers.size],
				deliveryDate = LocalDate.parse("2026-05-${(10 + index).toString().padStart(2, '0')}"),
				status = status,
				notes = "Seed order ${index + 1}",
				createdByUserId = actorUserId,
				createdAt = now.plusSeconds(index.toLong()),
				updatedAt = now.plusSeconds(index.toLong()),
				version = 0,
			)
			order.items.add(
				CustomerOrderItemEntity(
					id = UUID.randomUUID(),
					order = order,
					lineNo = 1,
					itemName = "Seed item ${index + 1}",
					quantity = BigDecimal(index + 1),
					uom = "шт",
					createdAt = now,
					updatedAt = now,
				),
			)
			val saved = orderRepository.save(order)
			statusChangeRepository.save(
				OrderStatusChangeEntity(
					id = UUID.randomUUID(),
					orderId = saved.id,
					fromStatus = null,
					toStatus = saved.status,
					actorUserId = actorUserId,
					changedAt = saved.createdAt,
					note = "Seed data",
				),
			)
			changeDiffRepository.save(
				OrderChangeDiffEntity(
					id = UUID.randomUUID(),
					orderId = saved.id,
					actorUserId = actorUserId,
					changedAt = saved.createdAt,
					changeType = OrderChangeType.CREATED,
					fieldDiffs = "",
				),
			)
			auditRepository.save(
				OrderAuditEventEntity(
					id = UUID.randomUUID(),
					eventType = "ORDER_CREATED",
					actorUserId = actorUserId,
					targetType = "CUSTOMER_ORDER",
					targetId = saved.id,
					eventAt = saved.createdAt,
					summary = "Seeded order ${saved.orderNumber}",
				),
			)
		}
	}
}

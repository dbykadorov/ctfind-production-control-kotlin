package com.ctfind.productioncontrol.orders.adapter.web

import com.ctfind.productioncontrol.orders.application.OrderListQuery
import com.ctfind.productioncontrol.orders.application.AuthenticatedOrderActor
import com.ctfind.productioncontrol.orders.application.CreateOrderCommand
import com.ctfind.productioncontrol.orders.application.CreateOrderUseCase
import com.ctfind.productioncontrol.orders.application.OrderMutationResult
import com.ctfind.productioncontrol.orders.application.OrderQueryUseCase
import com.ctfind.productioncontrol.orders.domain.OrderStatus
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/orders")
class OrderController(
	private val orderQuery: OrderQueryUseCase,
	private val createOrder: CreateOrderUseCase? = null,
) {
	@GetMapping
	fun list(
		@RequestParam(required = false) search: String? = null,
		@RequestParam(required = false) status: OrderStatus? = null,
		@RequestParam(required = false) customerId: UUID? = null,
		@RequestParam(required = false, defaultValue = "false") activeOnly: Boolean = false,
		@RequestParam(required = false, defaultValue = "false") overdueOnly: Boolean = false,
		@RequestParam(required = false) deliveryDateFrom: LocalDate? = null,
		@RequestParam(required = false) deliveryDateTo: LocalDate? = null,
		@RequestParam(required = false, defaultValue = "0") page: Int = 0,
		@RequestParam(required = false, defaultValue = "20") size: Int = 20,
	): OrderPageResponse =
		orderQuery.list(
			OrderListQuery(
				search = search,
				status = status,
				customerId = customerId,
				activeOnly = activeOnly,
				overdueOnly = overdueOnly,
				deliveryDateFrom = deliveryDateFrom,
				deliveryDateTo = deliveryDateTo,
				page = page,
				size = size,
			),
		).toOrderPageResponse()

	@GetMapping("/{id}")
	fun detail(@PathVariable id: UUID): ResponseEntity<OrderDetailResponse> =
		orderQuery.detail(id)
			?.let { ResponseEntity.ok(it.toResponse()) }
			?: ResponseEntity.notFound().build()

	@PostMapping
	fun create(
		@Valid @RequestBody request: CreateOrderRequest,
		@AuthenticationPrincipal jwt: Jwt,
	): ResponseEntity<Any> {
		val customerId = request.customerId
			?: return ResponseEntity.badRequest().body(OrderApiErrorResponse("validation_failed", "customerId is required"))
		val deliveryDate = request.deliveryDate
			?: return ResponseEntity.badRequest().body(OrderApiErrorResponse("validation_failed", "deliveryDate is required"))
		val useCase = createOrder ?: return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
		return when (
			val result = useCase.create(
				CreateOrderCommand(
					customerId = customerId,
					deliveryDate = deliveryDate,
					notes = request.notes,
					items = request.items.map { it.toCommand() },
					actor = jwt.toOrderActor(),
				),
			)
		) {
			is OrderMutationResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value.toResponse())
			OrderMutationResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(OrderApiErrorResponse("forbidden", "Order write access is required"))
			OrderMutationResult.NotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(OrderApiErrorResponse("not_found", "Order not found"))
			OrderMutationResult.StaleVersion -> ResponseEntity.status(HttpStatus.CONFLICT)
				.body(OrderApiErrorResponse("stale_order_version", "Order has changed. Reload before saving."))
			is OrderMutationResult.ValidationFailed -> ResponseEntity.badRequest()
				.body(OrderApiErrorResponse("validation_failed", result.message, result.field?.let { mapOf(it to result.message) } ?: emptyMap()))
			OrderMutationResult.InvalidTransition -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(OrderApiErrorResponse("invalid_status_transition", "Only direct forward status transitions are allowed."))
		}
	}
}

private fun Jwt.toOrderActor(): AuthenticatedOrderActor =
	AuthenticatedOrderActor(
		userId = (claims["userId"] as? String)
			?.let(UUID::fromString)
			?: UUID.nameUUIDFromBytes(subject.toByteArray()),
		login = subject,
		displayName = claims["displayName"] as? String ?: subject,
		roleCodes = roles(),
	)

@Suppress("UNCHECKED_CAST")
private fun Jwt.roles(): Set<String> =
	when (val raw = claims["roles"]) {
		is Collection<*> -> raw.filterIsInstance<String>().toSet()
		is String -> setOf(raw)
		else -> emptySet()
	}

package com.ctfind.productioncontrol.production.adapter.web

import com.ctfind.productioncontrol.production.application.ProductionTaskDetailQueryResult
import com.ctfind.productioncontrol.production.application.ProductionTaskListQuery
import com.ctfind.productioncontrol.production.application.ProductionTaskQueryUseCase
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/production-tasks")
class ProductionTaskController(
	private val query: ProductionTaskQueryUseCase,
) {

	@GetMapping
	fun list(
		@RequestParam(required = false) search: String? = null,
		@RequestParam(required = false) status: ProductionTaskStatus? = null,
		@RequestParam(required = false) orderId: UUID? = null,
		@RequestParam(required = false) orderItemId: UUID? = null,
		@RequestParam(required = false) executorUserId: UUID? = null,
		@RequestParam(required = false, defaultValue = "false") assignedToMe: Boolean = false,
		@RequestParam(required = false, defaultValue = "false") blockedOnly: Boolean = false,
		@RequestParam(required = false, defaultValue = "false") activeOnly: Boolean = false,
		@RequestParam(required = false) dueDateFrom: LocalDate? = null,
		@RequestParam(required = false) dueDateTo: LocalDate? = null,
		@RequestParam(required = false, defaultValue = "0") page: Int = 0,
		@RequestParam(required = false, defaultValue = "20") size: Int = 20,
		@RequestParam(required = false) sort: String? = null,
		@AuthenticationPrincipal jwt: Jwt,
	): ProductionTaskPageResponse {
		val result = query.list(
			ProductionTaskListQuery(
				search = search,
				status = status,
				orderId = orderId,
				orderItemId = orderItemId,
				executorUserId = executorUserId,
				assignedToMe = assignedToMe,
				blockedOnly = blockedOnly,
				activeOnly = activeOnly,
				dueDateFrom = dueDateFrom,
				dueDateTo = dueDateTo,
				page = page,
				size = size,
				sort = sort,
			),
			jwt.toProductionActor(),
		)
		return ProductionTaskPageResponse(
			items = result.items.map { it.toListItemResponse() },
			page = result.page,
			size = result.size,
			totalItems = result.totalItems,
			totalPages = result.totalPages,
		)
	}

	@GetMapping("/{id}")
	fun detail(
		@PathVariable id: UUID,
		@AuthenticationPrincipal jwt: Jwt,
	): ResponseEntity<Any> =
		when (val r = query.detail(id, jwt.toProductionActor())) {
			is ProductionTaskDetailQueryResult.Found ->
				ResponseEntity.ok(r.detail.toDetailResponse())
			ProductionTaskDetailQueryResult.NotFound -> ResponseEntity.notFound().build()
			ProductionTaskDetailQueryResult.Forbidden ->
				ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(ProductionTaskApiErrorResponse("forbidden", "Production task is not visible for the current user."))
		}
}
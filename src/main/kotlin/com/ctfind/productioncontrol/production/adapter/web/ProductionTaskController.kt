package com.ctfind.productioncontrol.production.adapter.web

import com.ctfind.productioncontrol.production.application.AssignProductionTaskCommand
import com.ctfind.productioncontrol.production.application.AssignProductionTaskUseCase
import com.ctfind.productioncontrol.production.application.ChangeProductionTaskStatusCommand
import com.ctfind.productioncontrol.production.application.ChangeProductionTaskStatusUseCase
import com.ctfind.productioncontrol.production.application.CreateProductionTaskDraft
import com.ctfind.productioncontrol.production.application.CreateProductionTasksFromOrderCommand
import com.ctfind.productioncontrol.production.application.CreateProductionTasksFromOrderUseCase
import com.ctfind.productioncontrol.production.application.ProductionTaskAssigneeQueryUseCase
import com.ctfind.productioncontrol.production.application.ProductionTaskDetailQueryResult
import com.ctfind.productioncontrol.production.application.ProductionTaskListQuery
import com.ctfind.productioncontrol.production.application.ProductionTaskMutationResult
import com.ctfind.productioncontrol.production.application.ProductionTaskQueryUseCase
import com.ctfind.productioncontrol.production.application.canAssignProductionTasks
import com.ctfind.productioncontrol.production.domain.ProductionTaskStatus
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/production-tasks")
class ProductionTaskController(
	private val query: ProductionTaskQueryUseCase,
	private val createFromOrder: CreateProductionTasksFromOrderUseCase,
	private val assign: AssignProductionTaskUseCase,
	private val changeStatus: ChangeProductionTaskStatusUseCase,
	private val assignees: ProductionTaskAssigneeQueryUseCase,
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

	@GetMapping("/assignees")
	fun listAssignees(
		@RequestParam(required = false) search: String? = null,
		@RequestParam(required = false, defaultValue = "20") limit: Int = 20,
		@AuthenticationPrincipal jwt: Jwt,
	): ResponseEntity<Any> {
		if (!canAssignProductionTasks(jwt.toProductionActor().roleCodes)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ProductionTaskApiErrorResponse("forbidden", "Listing assignees is not allowed for the current user."))
		}
		val items = assignees.search(search, limit).map { it.toAssigneeListItemResponse() }
		return ResponseEntity.ok(ProductionTaskAssigneesResponse(items = items))
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

	@PostMapping("/from-order")
	fun createFromOrder(
		@RequestBody body: CreateProductionTasksFromOrderRequest,
		@AuthenticationPrincipal jwt: Jwt,
	): ResponseEntity<Any> {
		val actor = jwt.toProductionActor()
		return when (val r = createFromOrder.execute(
			CreateProductionTasksFromOrderCommand(
				orderId = body.orderId,
				tasks = body.tasks.map { item ->
					CreateProductionTaskDraft(
						orderItemId = item.orderItemId,
						purpose = item.purpose,
						quantity = item.quantity,
						uom = item.uom,
						executorUserId = item.executorUserId,
						plannedStartDate = item.plannedStartDate,
						plannedFinishDate = item.plannedFinishDate,
					)
				},
				actorUserId = actor.userId,
				roleCodes = actor.roleCodes,
			),
		)) {
			is ProductionTaskMutationResult.Success ->
				ResponseEntity.status(HttpStatus.CREATED)
					.body(
						CreateProductionTasksFromOrderResponse(
							items = r.value.map { it.toCreatedItemResponse() },
						),
					)
			ProductionTaskMutationResult.Forbidden ->
				ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(ProductionTaskApiErrorResponse("forbidden", "Creating production tasks is not allowed for the current user."))
			is ProductionTaskMutationResult.ValidationFailed -> ResponseEntity.badRequest()
				.body(ProductionTaskApiErrorResponse(r.errorCode, r.message, r.details))
			else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
		}
	}

	@PutMapping("/{id}/assignment")
	fun putAssignment(
		@PathVariable id: UUID,
		@RequestBody body: PutProductionTaskAssignmentRequest,
		@AuthenticationPrincipal jwt: Jwt,
	): ResponseEntity<Any> {
		val actor = jwt.toProductionActor()
		return when (val r = assign.execute(
			AssignProductionTaskCommand(
				taskId = id,
				expectedVersion = body.expectedVersion,
				executorUserId = body.executorUserId,
				plannedStartDate = body.plannedStartDate,
				plannedFinishDate = body.plannedFinishDate,
				note = body.note,
				actorUserId = actor.userId,
				roleCodes = actor.roleCodes,
			),
		)) {
			is ProductionTaskMutationResult.Success -> responseDetailOrProblem(id, actor)
			ProductionTaskMutationResult.Forbidden ->
				ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(ProductionTaskApiErrorResponse("forbidden", "Assignment is not allowed for the current user."))
			ProductionTaskMutationResult.NotFound -> ResponseEntity.notFound().build()
			ProductionTaskMutationResult.StaleVersion ->
				ResponseEntity.status(HttpStatus.CONFLICT)
					.body(ProductionTaskApiErrorResponse("stale_production_task_version", "Production task has changed. Reload before saving."))
			is ProductionTaskMutationResult.ValidationFailed ->
				ResponseEntity.badRequest()
					.body(ProductionTaskApiErrorResponse(r.errorCode, r.message, r.details))
			else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
		}
	}

	@PostMapping("/{id}/status")
	fun postStatus(
		@PathVariable id: UUID,
		@RequestBody body: PostProductionTaskStatusRequest,
		@AuthenticationPrincipal jwt: Jwt,
	): ResponseEntity<Any> {
		val actor = jwt.toProductionActor()
		return when (val r = changeStatus.execute(
			ChangeProductionTaskStatusCommand(
				taskId = id,
				expectedVersion = body.expectedVersion,
				toStatus = body.toStatus,
				reason = body.reason,
				note = body.note,
				actorUserId = actor.userId,
				roleCodes = actor.roleCodes,
			),
		)) {
			is ProductionTaskMutationResult.Success -> responseDetailOrProblem(id, actor)
			ProductionTaskMutationResult.Forbidden ->
				ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(ProductionTaskApiErrorResponse("forbidden", "Status update is not allowed for the current user."))
			ProductionTaskMutationResult.NotFound -> ResponseEntity.notFound().build()
			ProductionTaskMutationResult.StaleVersion ->
				ResponseEntity.status(HttpStatus.CONFLICT)
					.body(ProductionTaskApiErrorResponse("stale_production_task_version", "Production task has changed. Reload before saving."))
			is ProductionTaskMutationResult.ValidationFailed ->
				ResponseEntity.badRequest()
					.body(ProductionTaskApiErrorResponse(r.errorCode, r.message, r.details))
			ProductionTaskMutationResult.InvalidTransition ->
				ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
					.body(ProductionTaskApiErrorResponse("invalid_task_status_transition", "Task status transition is not allowed."))
		}
	}

	private fun responseDetailOrProblem(
		id: UUID,
		actor: com.ctfind.productioncontrol.production.application.AuthenticatedProductionActor,
	): ResponseEntity<Any> =
		when (val d = query.detail(id, actor)) {
			is ProductionTaskDetailQueryResult.Found -> ResponseEntity.ok(d.detail.toDetailResponse())
			ProductionTaskDetailQueryResult.NotFound -> ResponseEntity.notFound().build()
			ProductionTaskDetailQueryResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ProductionTaskApiErrorResponse("forbidden", "Production task is not visible for the current user."))
		}
}

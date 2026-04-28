package com.ctfind.productioncontrol.production.adapter.web

import com.ctfind.productioncontrol.production.application.AssignProductionTaskCommand
import com.ctfind.productioncontrol.production.application.AssignProductionTaskUseCase
import com.ctfind.productioncontrol.production.application.AuthenticatedProductionActor
import com.ctfind.productioncontrol.production.application.ChangeProductionTaskStatusCommand
import com.ctfind.productioncontrol.production.application.ChangeProductionTaskStatusUseCase
import com.ctfind.productioncontrol.production.application.CreateProductionTasksFromOrderCommand
import com.ctfind.productioncontrol.production.application.CreateProductionTasksFromOrderUseCase
import com.ctfind.productioncontrol.production.application.CreatedProductionTaskSummary
import com.ctfind.productioncontrol.production.application.ProductionActorLookupPort
import com.ctfind.productioncontrol.production.application.ProductionExecutorPort
import com.ctfind.productioncontrol.production.application.ProductionOrderSourcePort
import com.ctfind.productioncontrol.production.application.ProductionTaskAssigneeQueryUseCase
import com.ctfind.productioncontrol.production.application.ProductionTaskAuditPort
import com.ctfind.productioncontrol.production.application.ProductionTaskAuditService
import com.ctfind.productioncontrol.production.application.ProductionTaskDetailQueryResult
import com.ctfind.productioncontrol.production.application.ProductionTaskDetailView
import com.ctfind.productioncontrol.production.application.ProductionTaskExecutorSummary
import com.ctfind.productioncontrol.production.application.ProductionTaskHistoryUseCase
import com.ctfind.productioncontrol.production.application.ProductionTaskListQuery
import com.ctfind.productioncontrol.production.application.ProductionTaskMutationResult
import com.ctfind.productioncontrol.production.application.ProductionTaskNumberPort
import com.ctfind.productioncontrol.production.application.ProductionTaskPageResult
import com.ctfind.productioncontrol.production.application.ProductionTaskPort
import com.ctfind.productioncontrol.production.application.ProductionTaskQueryUseCase
import com.ctfind.productioncontrol.production.application.ProductionTaskTracePort
import com.ctfind.productioncontrol.production.domain.ProductionTask
import com.ctfind.productioncontrol.production.domain.ProductionTaskAuditEvent
import com.ctfind.productioncontrol.production.domain.ProductionTaskHistoryEvent
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

internal fun jwtFor(actorId: UUID, roles: Set<String>): Jwt =
	Jwt.withTokenValue("tok")
		.header("alg", "none")
		.subject("user1")
		.claim("userId", actorId.toString())
		.claim("displayName", "Tester")
		.claim("roles", roles.toList())
		.build()

internal fun unusedTaskPort(): ProductionTaskPort = object : ProductionTaskPort {
	override fun findById(id: UUID): ProductionTask? = null
	override fun save(task: ProductionTask): ProductionTask = task
	override fun search(
		query: ProductionTaskListQuery,
		currentUserId: UUID?,
		roleCodes: Set<String>,
	): ProductionTaskPageResult<ProductionTask> = ProductionTaskPageResult(emptyList(), 0, 20, 0)
	override fun existsByOrderItemIdAndPurpose(orderItemId: UUID, purpose: String): Boolean = false
}

internal fun unusedOrderSourcePort(): ProductionOrderSourcePort = object : ProductionOrderSourcePort {
	override fun findOrderSource(orderId: UUID) = null
	override fun findOrderItemSource(orderId: UUID, orderItemId: UUID) = null
}

internal fun unusedExecutorPort(): ProductionExecutorPort = object : ProductionExecutorPort {
	override fun findExecutor(id: UUID): ProductionTaskExecutorSummary? = null
	override fun searchExecutors(search: String?, limit: Int): List<ProductionTaskExecutorSummary> = emptyList()
}

internal fun unusedNumberPort(): ProductionTaskNumberPort = object : ProductionTaskNumberPort {
	override fun nextTaskNumber(): String = "PT-UNUSED"
}

internal fun unusedTracePort(): ProductionTaskTracePort = object : ProductionTaskTracePort {
	override fun saveHistoryEvent(event: ProductionTaskHistoryEvent): ProductionTaskHistoryEvent = event
	override fun findHistoryEvents(taskId: UUID): List<ProductionTaskHistoryEvent> = emptyList()
}

internal fun unusedAuditService(): ProductionTaskAuditService =
	ProductionTaskAuditService(
		object : ProductionTaskAuditPort {
			override fun record(event: ProductionTaskAuditEvent): ProductionTaskAuditEvent = event
		},
	)

internal fun unusedActorLookup(): ProductionActorLookupPort = object : ProductionActorLookupPort {
	override fun displayName(userId: UUID): String? = null
}

internal fun unusedHistoryUseCase(): ProductionTaskHistoryUseCase =
	object : ProductionTaskHistoryUseCase(
		traces = unusedTracePort(),
		actorLookup = unusedActorLookup(),
	) {}

internal fun stubQuery(
	detailResult: ProductionTaskDetailQueryResult,
): ProductionTaskQueryUseCase = object : ProductionTaskQueryUseCase(
	tasks = unusedTaskPort(),
	orderSource = unusedOrderSourcePort(),
	executors = unusedExecutorPort(),
	history = unusedHistoryUseCase(),
) {
	override fun detail(taskId: UUID, actor: AuthenticatedProductionActor): ProductionTaskDetailQueryResult = detailResult
}

internal fun stubAssigneeQuery(
	results: List<ProductionTaskExecutorSummary> = emptyList(),
): ProductionTaskAssigneeQueryUseCase = object : ProductionTaskAssigneeQueryUseCase(executors = unusedExecutorPort()) {
	override fun search(search: String?, limit: Int): List<ProductionTaskExecutorSummary> = results
}

internal fun stubCreateUseCase(
	exec: (CreateProductionTasksFromOrderCommand) -> ProductionTaskMutationResult<List<CreatedProductionTaskSummary>>,
): CreateProductionTasksFromOrderUseCase = object : CreateProductionTasksFromOrderUseCase(
	tasks = unusedTaskPort(),
	orderSource = unusedOrderSourcePort(),
	executors = unusedExecutorPort(),
	numbers = unusedNumberPort(),
	traces = unusedTracePort(),
	audit = unusedAuditService(),
) {
	override fun execute(cmd: CreateProductionTasksFromOrderCommand) = exec(cmd)
}

internal fun stubAssignUseCase(
	exec: (AssignProductionTaskCommand) -> ProductionTaskMutationResult<Unit> =
		{ ProductionTaskMutationResult.NotFound },
): AssignProductionTaskUseCase = object : AssignProductionTaskUseCase(
	tasks = unusedTaskPort(),
	executors = unusedExecutorPort(),
	traces = unusedTracePort(),
	audit = unusedAuditService(),
) {
	override fun execute(cmd: AssignProductionTaskCommand) = exec(cmd)
}

internal fun stubChangeStatusUseCase(
	exec: (ChangeProductionTaskStatusCommand) -> ProductionTaskMutationResult<Unit> =
		{ ProductionTaskMutationResult.NotFound },
): ChangeProductionTaskStatusUseCase = object : ChangeProductionTaskStatusUseCase(
	tasks = unusedTaskPort(),
	traces = unusedTracePort(),
	audit = unusedAuditService(),
) {
	override fun execute(cmd: ChangeProductionTaskStatusCommand) = exec(cmd)
}

internal fun controllerWith(
	create: CreateProductionTasksFromOrderUseCase = stubCreateUseCase { ProductionTaskMutationResult.Forbidden },
	assign: AssignProductionTaskUseCase = stubAssignUseCase(),
	changeStatus: ChangeProductionTaskStatusUseCase = stubChangeStatusUseCase(),
	query: ProductionTaskQueryUseCase = stubQuery(ProductionTaskDetailQueryResult.NotFound),
	assignees: ProductionTaskAssigneeQueryUseCase = stubAssigneeQuery(),
): ProductionTaskController = ProductionTaskController(
	query = query,
	createFromOrder = create,
	assign = assign,
	changeStatus = changeStatus,
	assignees = assignees,
)

internal fun stubQueryReturning(detail: ProductionTaskDetailView): ProductionTaskQueryUseCase =
	stubQuery(ProductionTaskDetailQueryResult.Found(detail))

internal fun sampleDetailView(
	taskId: UUID = UUID.fromString("60000000-0000-0000-0000-000000000006"),
	status: com.ctfind.productioncontrol.production.domain.ProductionTaskStatus =
		com.ctfind.productioncontrol.production.domain.ProductionTaskStatus.NOT_STARTED,
	version: Long = 0,
): ProductionTaskDetailView =
	ProductionTaskDetailView(
		row = com.ctfind.productioncontrol.production.application.ProductionTaskListRowView(
			id = taskId,
			taskNumber = "PT-000001",
			purpose = "Раскрой",
			order = com.ctfind.productioncontrol.production.application.ProductionTaskOrderSummary(
				id = UUID.fromString("10000000-0000-0000-0000-000000000001"),
				orderNumber = "ORD-1",
				customerDisplayName = "Acme",
				status = com.ctfind.productioncontrol.orders.domain.OrderStatus.IN_WORK,
				deliveryDate = java.time.LocalDate.parse("2026-05-15"),
			),
			orderItem = null,
			quantity = java.math.BigDecimal("2"),
			uom = "шт",
			status = status,
			statusLabel = com.ctfind.productioncontrol.production.application.productionTaskStatusLabelRu(status),
			previousActiveStatus = null,
			executor = null,
			plannedStartDate = null,
			plannedFinishDate = null,
			blockedReason = null,
			updatedAt = java.time.Instant.parse("2026-04-27T10:00:00Z"),
			version = version,
		),
		allowedActions = emptySet(),
		history = emptyList(),
		createdAt = java.time.Instant.parse("2026-04-27T10:00:00Z"),
	)

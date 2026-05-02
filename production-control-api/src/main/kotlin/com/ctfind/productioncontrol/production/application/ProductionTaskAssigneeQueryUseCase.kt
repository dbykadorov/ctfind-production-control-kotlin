package com.ctfind.productioncontrol.production.application

import org.springframework.stereotype.Service

@Service
class ProductionTaskAssigneeQueryUseCase(
	private val executors: ProductionExecutorPort,
) {
	fun search(search: String?, limit: Int): List<ProductionTaskExecutorSummary> =
		executors.searchExecutors(search, limit)
}

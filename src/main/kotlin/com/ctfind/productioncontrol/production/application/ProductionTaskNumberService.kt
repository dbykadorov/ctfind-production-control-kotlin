package com.ctfind.productioncontrol.production.application

import org.springframework.stereotype.Service

@Service
class ProductionTaskNumberService(
	private val numbers: ProductionTaskNumberPort,
) {
	fun nextTaskNumber(): String = numbers.nextTaskNumber()
}

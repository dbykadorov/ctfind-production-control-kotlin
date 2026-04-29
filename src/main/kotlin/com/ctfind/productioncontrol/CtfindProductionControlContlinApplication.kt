package com.ctfind.productioncontrol

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CtfindProductionControlContlinApplication

fun main(args: Array<String>) {
	runApplication<CtfindProductionControlContlinApplication>(*args)
}

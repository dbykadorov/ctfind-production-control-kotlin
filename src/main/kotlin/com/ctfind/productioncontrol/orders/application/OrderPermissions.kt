package com.ctfind.productioncontrol.orders.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE

const val ORDER_MANAGER_ROLE_CODE = "ORDER_MANAGER"

fun canWriteOrders(roleCodes: Set<String>): Boolean =
	roleCodes.any { it == ADMIN_ROLE_CODE || it == ORDER_MANAGER_ROLE_CODE }

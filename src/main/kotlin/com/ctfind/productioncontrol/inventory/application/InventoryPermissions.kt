package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE
import com.ctfind.productioncontrol.orders.application.ORDER_MANAGER_ROLE_CODE
import com.ctfind.productioncontrol.production.application.PRODUCTION_SUPERVISOR_ROLE_CODE

const val WAREHOUSE_ROLE_CODE = "WAREHOUSE"

fun canManageInventory(roleCodes: Set<String>): Boolean =
    roleCodes.any { it == ADMIN_ROLE_CODE || it == WAREHOUSE_ROLE_CODE }

fun canEditBom(roleCodes: Set<String>): Boolean =
    roleCodes.any { it == ADMIN_ROLE_CODE || it == ORDER_MANAGER_ROLE_CODE }

fun canConsumeStock(roleCodes: Set<String>): Boolean =
    roleCodes.any { it == ADMIN_ROLE_CODE || it == WAREHOUSE_ROLE_CODE }

fun canViewBomAndUsage(roleCodes: Set<String>): Boolean =
    roleCodes.any {
        it == ADMIN_ROLE_CODE ||
            it == ORDER_MANAGER_ROLE_CODE ||
            it == WAREHOUSE_ROLE_CODE ||
            it == PRODUCTION_SUPERVISOR_ROLE_CODE
    }

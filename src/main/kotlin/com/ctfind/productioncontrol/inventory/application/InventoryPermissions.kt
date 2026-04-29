package com.ctfind.productioncontrol.inventory.application

import com.ctfind.productioncontrol.auth.domain.ADMIN_ROLE_CODE

const val WAREHOUSE_ROLE_CODE = "WAREHOUSE"

fun canManageInventory(roleCodes: Set<String>): Boolean =
    roleCodes.any { it == ADMIN_ROLE_CODE || it == WAREHOUSE_ROLE_CODE }

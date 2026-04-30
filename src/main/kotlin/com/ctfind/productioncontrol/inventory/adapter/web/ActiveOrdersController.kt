package com.ctfind.productioncontrol.inventory.adapter.web

import com.ctfind.productioncontrol.inventory.application.ActiveOrderSearchQuery
import com.ctfind.productioncontrol.inventory.application.AuthenticatedInventoryActor
import com.ctfind.productioncontrol.inventory.application.OrderLookupPort
import com.ctfind.productioncontrol.inventory.application.canConsumeStock
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/orders/active-for-consumption")
class ActiveOrdersController(
    private val orderLookup: OrderLookupPort,
) {
    @GetMapping
    fun list(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false, defaultValue = "20") limit: Int,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Any> {
        val actor = jwt.toActor()
        if (!canConsumeStock(actor.roleCodes)) {
            return ResponseEntity.status(403).body(InventoryApiError("forbidden", "Warehouse write access is required"))
        }

        val items = orderLookup.searchActiveOrdersForConsumption(
            ActiveOrderSearchQuery(search = search, limit = limit),
        ).map { it.toResponse() }

        return ResponseEntity.ok(InventoryOrderListResponse(items = items))
    }

    private fun Jwt.toActor(): AuthenticatedInventoryActor =
        AuthenticatedInventoryActor(
            userId = (claims["userId"] as? String)?.let(UUID::fromString)
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
}

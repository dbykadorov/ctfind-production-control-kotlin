package com.ctfind.productioncontrol.inventory.adapter.web

import com.ctfind.productioncontrol.inventory.application.AuthenticatedInventoryActor
import com.ctfind.productioncontrol.inventory.application.InventoryQueryUseCase
import com.ctfind.productioncontrol.inventory.application.canViewBomAndUsage
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/orders/{orderId}/material-usage")
class MaterialUsageController(
    private val query: InventoryQueryUseCase,
) {
    @GetMapping
    fun getUsage(
        @PathVariable orderId: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Any> {
        val actor = jwt.toActor()
        if (!canViewBomAndUsage(actor.roleCodes)) {
            return ResponseEntity.status(403).body(InventoryApiError("forbidden", "Order material usage access is required"))
        }
        val usage = query.getMaterialUsage(orderId)
            ?: return ResponseEntity.status(404).body(InventoryApiError("order_not_found", "Order not found"))
        return ResponseEntity.ok(usage.toResponse())
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

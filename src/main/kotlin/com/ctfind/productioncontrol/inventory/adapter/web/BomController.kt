package com.ctfind.productioncontrol.inventory.adapter.web

import com.ctfind.productioncontrol.inventory.application.AddBomLineCommand
import com.ctfind.productioncontrol.inventory.application.AddBomLineUseCase
import com.ctfind.productioncontrol.inventory.application.AuthenticatedInventoryActor
import com.ctfind.productioncontrol.inventory.application.InventoryMutationResult
import com.ctfind.productioncontrol.inventory.application.InventoryQueryUseCase
import com.ctfind.productioncontrol.inventory.application.RemoveBomLineCommand
import com.ctfind.productioncontrol.inventory.application.RemoveBomLineUseCase
import com.ctfind.productioncontrol.inventory.application.UpdateBomLineCommand
import com.ctfind.productioncontrol.inventory.application.UpdateBomLineUseCase
import com.ctfind.productioncontrol.inventory.application.canViewBomAndUsage
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/orders/{orderId}/bom")
class BomController(
    private val query: InventoryQueryUseCase,
    private val addBomLine: AddBomLineUseCase,
    private val updateBomLine: UpdateBomLineUseCase,
    private val removeBomLine: RemoveBomLineUseCase,
) {
    @GetMapping
    fun list(
        @PathVariable orderId: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Any> {
        val actor = jwt.toActor()
        if (!canViewBomAndUsage(actor.roleCodes)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(InventoryApiError("forbidden", "Order BOM visibility access is required"))
        }

        return query.listBom(orderId)
            ?.let { ResponseEntity.ok(BomLineListResponse(it.map { row -> row.toResponse() })) }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("order_not_found", "Order not found"))
    }

    @PostMapping
    fun add(
        @PathVariable orderId: UUID,
        @RequestBody body: BomLineCreateRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Any> =
        when (
            val result = addBomLine.add(
                AddBomLineCommand(
                    orderId = orderId,
                    materialId = body.materialId,
                    quantity = body.quantity,
                    comment = body.comment,
                    actor = jwt.toActor(),
                ),
            )
        ) {
            is InventoryMutationResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value.toResponse())
            InventoryMutationResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(InventoryApiError("forbidden", "Order BOM write access is required"))
            InventoryMutationResult.OrderNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("order_not_found", "Order not found"))
            InventoryMutationResult.MaterialNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("material_not_found", "Material not found"))
            InventoryMutationResult.OrderLocked -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("order_locked", "Order is locked for inventory changes"))
            is InventoryMutationResult.ValidationFailed -> ResponseEntity.badRequest()
                .body(InventoryApiError("validation_failed", result.message, result.field))
            is InventoryMutationResult.Conflict -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("bom_line_duplicate", result.message))
            else -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("conflict", "Operation cannot be completed"))
        }

    @PutMapping("/{lineId}")
    fun update(
        @PathVariable orderId: UUID,
        @PathVariable lineId: UUID,
        @RequestBody body: BomLineUpdateRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Any> =
        when (
            val result = updateBomLine.update(
                UpdateBomLineCommand(
                    orderId = orderId,
                    lineId = lineId,
                    quantity = body.quantity,
                    comment = body.comment,
                    actor = jwt.toActor(),
                ),
            )
        ) {
            is InventoryMutationResult.Success -> ResponseEntity.ok(result.value.toResponse())
            InventoryMutationResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(InventoryApiError("forbidden", "Order BOM write access is required"))
            InventoryMutationResult.OrderNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("order_not_found", "Order not found"))
            InventoryMutationResult.MaterialNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("material_not_found", "Material not found"))
            InventoryMutationResult.BomLineNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("bom_line_not_found", "BOM line not found"))
            InventoryMutationResult.OrderLocked -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("order_locked", "Order is locked for inventory changes"))
            is InventoryMutationResult.ValidationFailed -> ResponseEntity.badRequest()
                .body(InventoryApiError("validation_failed", result.message, result.field))
            is InventoryMutationResult.Conflict -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("conflict", result.message))
            else -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("conflict", "Operation cannot be completed"))
        }

    @DeleteMapping("/{lineId}")
    fun remove(
        @PathVariable orderId: UUID,
        @PathVariable lineId: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Any> =
        when (
            val result = removeBomLine.remove(
                RemoveBomLineCommand(
                    orderId = orderId,
                    lineId = lineId,
                    actor = jwt.toActor(),
                ),
            )
        ) {
            is InventoryMutationResult.Success -> ResponseEntity.noContent().build()
            InventoryMutationResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(InventoryApiError("forbidden", "Order BOM write access is required"))
            InventoryMutationResult.OrderNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("order_not_found", "Order not found"))
            InventoryMutationResult.MaterialNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("material_not_found", "Material not found"))
            InventoryMutationResult.BomLineNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("bom_line_not_found", "BOM line not found"))
            InventoryMutationResult.OrderLocked -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("order_locked", "Order is locked for inventory changes"))
            is InventoryMutationResult.Conflict -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("bom_line_has_consumption", result.message))
            else -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("conflict", "Operation cannot be completed"))
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

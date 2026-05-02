package com.ctfind.productioncontrol.inventory.adapter.web

import com.ctfind.productioncontrol.inventory.application.AuthenticatedInventoryActor
import com.ctfind.productioncontrol.inventory.application.CreateMaterialCommand
import com.ctfind.productioncontrol.inventory.application.CreateMaterialUseCase
import com.ctfind.productioncontrol.inventory.application.DeleteMaterialCommand
import com.ctfind.productioncontrol.inventory.application.DeleteMaterialUseCase
import com.ctfind.productioncontrol.inventory.application.ConsumeStockCommand
import com.ctfind.productioncontrol.inventory.application.ConsumeStockUseCase
import com.ctfind.productioncontrol.inventory.application.InventoryMutationResult
import com.ctfind.productioncontrol.inventory.application.InventoryQueryUseCase
import com.ctfind.productioncontrol.inventory.application.MaterialListQuery
import com.ctfind.productioncontrol.inventory.application.ReceiveStockCommand
import com.ctfind.productioncontrol.inventory.application.ReceiveStockUseCase
import com.ctfind.productioncontrol.inventory.application.UpdateMaterialCommand
import com.ctfind.productioncontrol.inventory.application.UpdateMaterialUseCase
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/api/materials")
class InventoryController(
    private val query: InventoryQueryUseCase,
    private val createMaterial: CreateMaterialUseCase,
    private val updateMaterial: UpdateMaterialUseCase,
    private val deleteMaterial: DeleteMaterialUseCase,
    private val receiveStock: ReceiveStockUseCase,
    private val consumeStock: ConsumeStockUseCase,
) {
    @GetMapping
    fun list(
        @RequestParam(required = false) search: String? = null,
        @RequestParam(required = false, defaultValue = "0") page: Int = 0,
        @RequestParam(required = false, defaultValue = "20") size: Int = 20,
    ): MaterialsPageResponse =
        query.listMaterials(MaterialListQuery(search = search, page = page, size = size)).toResponse()

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: UUID): ResponseEntity<MaterialResponse> =
        query.getMaterial(id)?.let { ResponseEntity.ok(it.toResponse()) }
            ?: ResponseEntity.notFound().build()

    @PostMapping
    fun create(
        @RequestBody request: CreateMaterialRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Any> =
        when (val result = createMaterial.create(CreateMaterialCommand(request.name, request.unit, jwt.toActor()))) {
            is InventoryMutationResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value.toResponse())
            InventoryMutationResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(InventoryApiError("forbidden", "Warehouse write access is required"))
            InventoryMutationResult.NotFound -> ResponseEntity.notFound().build()
            InventoryMutationResult.OrderNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("order_not_found", "Order not found"))
            InventoryMutationResult.MaterialNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("material_not_found", "Material not found"))
            InventoryMutationResult.BomLineNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("bom_line_not_found", "BOM line not found"))
            InventoryMutationResult.OrderLocked -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("order_locked", "Order is locked for inventory changes"))
            InventoryMutationResult.MaterialNotInBom -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("material_not_in_bom", "Material is not part of order BOM"))
            is InventoryMutationResult.InsufficientStock -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("insufficient_stock", "Insufficient stock", available = result.available))
            is InventoryMutationResult.ValidationFailed -> ResponseEntity.badRequest()
                .body(InventoryApiError("validation_failed", result.message, result.field))
            is InventoryMutationResult.Conflict -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("conflict", result.message))
        }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateMaterialRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Any> =
        when (val result = updateMaterial.update(UpdateMaterialCommand(id, request.name, request.unit, jwt.toActor()))) {
            is InventoryMutationResult.Success -> ResponseEntity.ok(result.value.toResponse())
            InventoryMutationResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(InventoryApiError("forbidden", "Warehouse write access is required"))
            InventoryMutationResult.NotFound -> ResponseEntity.notFound().build()
            InventoryMutationResult.OrderNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("order_not_found", "Order not found"))
            InventoryMutationResult.MaterialNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("material_not_found", "Material not found"))
            InventoryMutationResult.BomLineNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("bom_line_not_found", "BOM line not found"))
            InventoryMutationResult.OrderLocked -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("order_locked", "Order is locked for inventory changes"))
            InventoryMutationResult.MaterialNotInBom -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("material_not_in_bom", "Material is not part of order BOM"))
            is InventoryMutationResult.InsufficientStock -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("insufficient_stock", "Insufficient stock", available = result.available))
            is InventoryMutationResult.ValidationFailed -> ResponseEntity.badRequest()
                .body(InventoryApiError("validation_failed", result.message, result.field))
            is InventoryMutationResult.Conflict -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("conflict", result.message))
        }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Any> =
        when (val result = deleteMaterial.delete(DeleteMaterialCommand(id, jwt.toActor()))) {
            is InventoryMutationResult.Success -> ResponseEntity.noContent().build()
            InventoryMutationResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(InventoryApiError("forbidden", "Warehouse write access is required"))
            InventoryMutationResult.NotFound -> ResponseEntity.notFound().build()
            InventoryMutationResult.OrderNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("order_not_found", "Order not found"))
            InventoryMutationResult.MaterialNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("material_not_found", "Material not found"))
            InventoryMutationResult.BomLineNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("bom_line_not_found", "BOM line not found"))
            InventoryMutationResult.OrderLocked -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("order_locked", "Order is locked for inventory changes"))
            InventoryMutationResult.MaterialNotInBom -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("material_not_in_bom", "Material is not part of order BOM"))
            is InventoryMutationResult.InsufficientStock -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("insufficient_stock", "Insufficient stock", available = result.available))
            is InventoryMutationResult.ValidationFailed -> ResponseEntity.badRequest()
                .body(InventoryApiError("validation_failed", result.message, result.field))
            is InventoryMutationResult.Conflict -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("conflict", result.message))
        }

    @PostMapping("/{id}/receipt")
    fun receipt(
        @PathVariable id: UUID,
        @RequestBody request: StockReceiptRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Any> =
        when (val result = receiveStock.receive(ReceiveStockCommand(id, request.quantity, request.comment, jwt.toActor()))) {
            is InventoryMutationResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value.toResponse())
            InventoryMutationResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(InventoryApiError("forbidden", "Warehouse write access is required"))
            InventoryMutationResult.NotFound -> ResponseEntity.notFound().build()
            InventoryMutationResult.OrderNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("order_not_found", "Order not found"))
            InventoryMutationResult.MaterialNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("material_not_found", "Material not found"))
            InventoryMutationResult.BomLineNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("bom_line_not_found", "BOM line not found"))
            InventoryMutationResult.OrderLocked -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("order_locked", "Order is locked for inventory changes"))
            InventoryMutationResult.MaterialNotInBom -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("material_not_in_bom", "Material is not part of order BOM"))
            is InventoryMutationResult.InsufficientStock -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("insufficient_stock", "Insufficient stock", available = result.available))
            is InventoryMutationResult.ValidationFailed -> ResponseEntity.badRequest()
                .body(InventoryApiError("validation_failed", result.message, result.field))
            is InventoryMutationResult.Conflict -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("conflict", result.message))
        }

    @PostMapping("/{id}/consume")
    fun consume(
        @PathVariable id: UUID,
        @RequestBody request: ConsumeRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Any> =
        when (
            val result = consumeStock.consume(
                ConsumeStockCommand(
                    materialId = id,
                    orderId = request.orderId,
                    quantity = request.quantity,
                    comment = request.comment,
                    actor = jwt.toActor(),
                ),
            )
        ) {
            is InventoryMutationResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.value.toResponse())
            InventoryMutationResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(InventoryApiError("forbidden", "Warehouse write access is required"))
            InventoryMutationResult.NotFound -> ResponseEntity.notFound().build()
            InventoryMutationResult.OrderNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("order_not_found", "Order not found"))
            InventoryMutationResult.MaterialNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("material_not_found", "Material not found"))
            InventoryMutationResult.BomLineNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InventoryApiError("bom_line_not_found", "BOM line not found"))
            InventoryMutationResult.OrderLocked -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("order_locked", "Order is locked for inventory changes"))
            InventoryMutationResult.MaterialNotInBom -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("material_not_in_bom", "Material is not part of order BOM"))
            is InventoryMutationResult.InsufficientStock -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("insufficient_stock", "Insufficient stock", available = result.available))
            is InventoryMutationResult.ValidationFailed -> ResponseEntity.badRequest()
                .body(InventoryApiError("validation_failed", result.message, result.field))
            is InventoryMutationResult.Conflict -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InventoryApiError("conflict", result.message))
        }

    @GetMapping("/{id}/movements")
    fun movements(
        @PathVariable id: UUID,
        @RequestParam(required = false, defaultValue = "0") page: Int = 0,
        @RequestParam(required = false, defaultValue = "20") size: Int = 20,
    ): ResponseEntity<StockMovementsPageResponse> =
        query.listMovements(id, page, size)?.let { ResponseEntity.ok(it.toResponse()) }
            ?: ResponseEntity.notFound().build()

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

data class InventoryApiError(
    val error: String,
    val message: String,
    val field: String? = null,
    val available: BigDecimal? = null,
)

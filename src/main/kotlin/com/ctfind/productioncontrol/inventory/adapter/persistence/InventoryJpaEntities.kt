package com.ctfind.productioncontrol.inventory.adapter.persistence

import com.ctfind.productioncontrol.inventory.domain.MeasurementUnit
import com.ctfind.productioncontrol.inventory.domain.MovementType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "material")
class MaterialEntity(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    var name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var unit: MeasurementUnit = MeasurementUnit.PIECE,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.EPOCH,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
)

@Entity
@Table(name = "stock_movement")
class StockMovementEntity(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(name = "material_id", nullable = false)
    var materialId: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    var movementType: MovementType = MovementType.RECEIPT,

    @Column(nullable = false, precision = 19, scale = 4)
    var quantity: BigDecimal = BigDecimal.ZERO,

    @Column
    var comment: String? = null,

    @Column(name = "actor_user_id", nullable = false)
    var actorUserId: UUID = UUID.randomUUID(),

    @Column(name = "actor_display_name", nullable = false)
    var actorDisplayName: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.EPOCH,
)

@Entity
@Table(name = "inventory_audit_event")
class InventoryAuditEventEntity(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(name = "event_type", nullable = false)
    var eventType: String = "",

    @Column(name = "actor_user_id", nullable = false)
    var actorUserId: UUID = UUID.randomUUID(),

    @Column(name = "target_id")
    var targetId: UUID? = null,

    @Column(name = "event_at", nullable = false)
    var eventAt: Instant = Instant.EPOCH,

    @Column(nullable = false)
    var summary: String = "",

    @Column(columnDefinition = "text")
    var metadata: String? = null,
)

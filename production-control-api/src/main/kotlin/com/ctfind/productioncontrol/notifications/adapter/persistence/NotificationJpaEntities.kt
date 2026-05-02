package com.ctfind.productioncontrol.notifications.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notification")
class NotificationEntity(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(name = "recipient_user_id", nullable = false)
	var recipientUserId: UUID = UUID.randomUUID(),

	@Column(nullable = false, length = 50)
	var type: String = "",

	@Column(nullable = false, length = 200)
	var title: String = "",

	@Column(length = 1000)
	var body: String? = null,

	@Column(name = "target_type", length = 30)
	var targetType: String? = null,

	@Column(name = "target_id", length = 100)
	var targetId: String? = null,

	@Column(name = "target_entity_id")
	var targetEntityId: UUID? = null,

	@Column(nullable = false)
	var read: Boolean = false,

	@Column(name = "read_at")
	var readAt: Instant? = null,

	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.now(),
)

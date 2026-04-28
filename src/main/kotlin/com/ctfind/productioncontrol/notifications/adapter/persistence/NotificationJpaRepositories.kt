package com.ctfind.productioncontrol.notifications.adapter.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface NotificationJpaRepository : JpaRepository<NotificationEntity, UUID> {

	fun findByRecipientUserId(recipientUserId: UUID, pageable: Pageable): Page<NotificationEntity>

	fun findByRecipientUserIdAndReadFalse(recipientUserId: UUID, pageable: Pageable): Page<NotificationEntity>

	fun countByRecipientUserIdAndReadFalse(recipientUserId: UUID): Long

	@Modifying
	@Query(
		"UPDATE NotificationEntity n SET n.read = true, n.readAt = :readAt " +
			"WHERE n.recipientUserId = :recipientUserId AND n.read = false",
	)
	fun markAllReadByRecipientUserId(
		@Param("recipientUserId") recipientUserId: UUID,
		@Param("readAt") readAt: Instant,
	): Int
}

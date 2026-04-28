package com.ctfind.productioncontrol.notifications.adapter.web

import com.ctfind.productioncontrol.notifications.application.ListNotificationsUseCase
import com.ctfind.productioncontrol.notifications.application.MarkNotificationReadUseCase
import com.ctfind.productioncontrol.notifications.application.NotificationListQuery
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
	private val listUseCase: ListNotificationsUseCase,
	private val markReadUseCase: MarkNotificationReadUseCase,
) {

	@GetMapping
	fun list(
		@AuthenticationPrincipal jwt: Jwt,
		@RequestParam(required = false, defaultValue = "0") page: Int,
		@RequestParam(required = false, defaultValue = "20") size: Int,
		@RequestParam(required = false, defaultValue = "false") unreadOnly: Boolean,
	): NotificationPageResponse {
		val clampedSize = size.coerceIn(1, 100)
		val result = listUseCase.list(
			NotificationListQuery(
				recipientUserId = jwt.extractUserId(),
				unreadOnly = unreadOnly,
				page = page,
				size = clampedSize,
			),
		)
		return NotificationPageResponse(
			items = result.items.map { it.toResponse() },
			page = result.page,
			size = result.size,
			totalItems = result.totalItems,
			totalPages = result.totalPages,
		)
	}

	@GetMapping("/unread-count")
	fun unreadCount(@AuthenticationPrincipal jwt: Jwt): UnreadCountResponse =
		UnreadCountResponse(count = listUseCase.countUnread(jwt.extractUserId()))

	@PatchMapping("/{id}/read")
	fun markRead(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
	): ResponseEntity<MarkReadResponse> {
		val notification = markReadUseCase.markRead(id, jwt.extractUserId())
			?: return ResponseEntity.notFound().build()
		return ResponseEntity.ok(
			MarkReadResponse(
				id = notification.id,
				read = notification.read,
				readAt = notification.readAt,
			),
		)
	}

	@PostMapping("/mark-all-read")
	fun markAllRead(@AuthenticationPrincipal jwt: Jwt): MarkAllReadResponse =
		MarkAllReadResponse(updated = markReadUseCase.markAllRead(jwt.extractUserId()))
}

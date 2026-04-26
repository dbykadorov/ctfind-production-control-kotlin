package com.ctfind.productioncontrol.auth.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "app_user")
class UserAccountEntity(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(nullable = false, unique = true)
	var login: String = "",

	@Column(name = "display_name", nullable = false)
	var displayName: String = "",

	@Column(name = "password_hash", nullable = false)
	var passwordHash: String = "",

	@Column(nullable = false)
	var enabled: Boolean = true,

	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.EPOCH,

	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant = Instant.EPOCH,

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "app_user_role",
		joinColumns = [JoinColumn(name = "user_id")],
		inverseJoinColumns = [JoinColumn(name = "role_id")],
	)
	var roles: MutableSet<RoleEntity> = linkedSetOf(),
)

@Entity
@Table(name = "app_role")
class RoleEntity(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(nullable = false, unique = true)
	var code: String = "",

	@Column(nullable = false)
	var name: String = "",

	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.EPOCH,
)

@Entity
@Table(name = "auth_audit_event")
class AuthenticationAuditEventEntity(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(name = "event_type", nullable = false)
	var eventType: String = "",

	@Column(nullable = false)
	var outcome: String = "",

	@Column
	var login: String? = null,

	@Column(name = "user_id")
	var userId: UUID? = null,

	@Column(name = "request_ip")
	var requestIp: String? = null,

	@Column(name = "user_agent")
	var userAgent: String? = null,

	@Column(name = "occurred_at", nullable = false)
	var occurredAt: Instant = Instant.EPOCH,

	@Column
	var details: String? = null,
)

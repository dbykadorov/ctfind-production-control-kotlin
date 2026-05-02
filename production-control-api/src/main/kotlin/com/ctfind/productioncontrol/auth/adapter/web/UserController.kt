package com.ctfind.productioncontrol.auth.adapter.web

import com.ctfind.productioncontrol.auth.application.CreateUserCommand
import com.ctfind.productioncontrol.auth.application.CreateUserResult
import com.ctfind.productioncontrol.auth.application.CreateUserUseCase
import com.ctfind.productioncontrol.auth.application.RoleCatalogResult
import com.ctfind.productioncontrol.auth.application.RoleCatalogUseCase
import com.ctfind.productioncontrol.auth.application.UpdateUserCommand
import com.ctfind.productioncontrol.auth.application.UpdateUserResult
import com.ctfind.productioncontrol.auth.application.UpdateUserUseCase
import com.ctfind.productioncontrol.auth.application.UserSummary
import com.ctfind.productioncontrol.auth.application.UserQueryResult
import com.ctfind.productioncontrol.auth.application.UserQueryUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/users")
class UserController(
    private val queryUseCase: UserQueryUseCase,
    private val createUserUseCase: CreateUserUseCase,
    private val roleCatalogUseCase: RoleCatalogUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false, defaultValue = "50") limit: Int,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<*> {
        val roleCodes = jwtRoles(jwt)
        val cappedLimit = limit.coerceIn(1, 100)
        return when (val result = queryUseCase.search(search, cappedLimit, roleCodes)) {
            is UserQueryResult.Success -> ResponseEntity.ok(
                result.users.map { it.toResponse() },
            )
            is UserQueryResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(AuthErrorResponse("forbidden", "Access denied"))
        }
    }

    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateUserRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<*> {
        val roleCodes = jwtRoles(jwt)
        val result = createUserUseCase.create(
            CreateUserCommand(
                login = request.login,
                displayName = request.displayName,
                initialPassword = request.initialPassword,
                roleCodes = request.roleCodes,
                actorRoleCodes = roleCodes,
                actorLogin = jwt.subject,
                actorUserId = jwtUserId(jwt),
            ),
        )
        return when (result) {
            is CreateUserResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(result.user.toResponse())
            is CreateUserResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(AuthErrorResponse("forbidden", "Access denied"))
            is CreateUserResult.DuplicateLogin -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(AuthErrorResponse("duplicate_login", "User login already exists"))
            is CreateUserResult.InvalidRoles -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(AuthErrorResponse("invalid_roles", "Unknown role codes: ${result.roleCodes.sorted().joinToString(",")}"))
            is CreateUserResult.ValidationError -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(AuthErrorResponse("validation_error", result.message))
        }
    }

    @GetMapping("/roles")
    fun listRoles(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<*> {
        val roleCodes = jwtRoles(jwt)
        return when (val result = roleCatalogUseCase.list(roleCodes)) {
            is RoleCatalogResult.Success -> ResponseEntity.ok(
                result.roles.map { RoleSummaryResponse(code = it.code, name = it.name) },
            )
            is RoleCatalogResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(AuthErrorResponse("forbidden", "Access denied"))
        }
    }

    @PutMapping("/{userId}")
    fun update(
        @PathVariable userId: java.util.UUID,
        @Valid @RequestBody request: UpdateUserRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<*> {
        val roleCodes = jwtRoles(jwt)
        val result = updateUserUseCase.update(
            UpdateUserCommand(
                userId = userId,
                displayName = request.displayName,
                roleCodes = request.roleCodes,
                actorRoleCodes = roleCodes,
                actorLogin = jwt.subject,
                actorUserId = jwtUserId(jwt),
            ),
        )
        return when (result) {
            is UpdateUserResult.Success -> ResponseEntity.ok(result.user.toResponse())
            is UpdateUserResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(AuthErrorResponse("forbidden", "Access denied"))
            is UpdateUserResult.NotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(AuthErrorResponse("user_not_found", "User not found"))
            is UpdateUserResult.LastAdminRoleRemovalForbidden -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                    AuthErrorResponse(
                        "last_admin_role_removal_forbidden",
                        "Cannot remove ADMIN role from the last active administrator",
                    ),
                )
            is UpdateUserResult.InvalidRoles -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(AuthErrorResponse("invalid_roles", "Unknown role codes: ${result.roleCodes.sorted().joinToString(",")}"))
            is UpdateUserResult.ValidationError -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(AuthErrorResponse("validation_error", result.message))
        }
    }

    private fun jwtRoles(jwt: Jwt): Set<String> {
        val roles = jwt.claims["roles"]
        return when (roles) {
            is Collection<*> -> roles.filterIsInstance<String>().toSet()
            else -> emptySet()
        }
    }

    private fun jwtUserId(jwt: Jwt): java.util.UUID? =
        (jwt.claims["userId"] as? String)?.let {
            runCatching { java.util.UUID.fromString(it) }.getOrNull()
        }

    private fun UserSummary.toResponse(): UserSummaryResponse =
        UserSummaryResponse(
            id = id,
            login = login,
            displayName = displayName,
            roles = roles.map { RoleSummaryResponse(code = it.code, name = it.name) },
        )
}

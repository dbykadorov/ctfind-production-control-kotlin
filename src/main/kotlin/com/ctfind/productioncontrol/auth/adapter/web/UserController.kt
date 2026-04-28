package com.ctfind.productioncontrol.auth.adapter.web

import com.ctfind.productioncontrol.auth.application.UserQueryResult
import com.ctfind.productioncontrol.auth.application.UserQueryUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val useCase: UserQueryUseCase,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false, defaultValue = "50") limit: Int,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<*> {
        val roleCodes = jwtRoles(jwt)
        val cappedLimit = limit.coerceIn(1, 100)
        return when (val result = useCase.search(search, cappedLimit, roleCodes)) {
            is UserQueryResult.Success -> ResponseEntity.ok(
                result.users.map {
                    UserSummaryResponse(id = it.id, login = it.login, displayName = it.displayName)
                },
            )
            is UserQueryResult.Forbidden -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(AuthErrorResponse("forbidden", "Access denied"))
        }
    }

    private fun jwtRoles(jwt: Jwt): Set<String> {
        val roles = jwt.claims["roles"]
        return when (roles) {
            is Collection<*> -> roles.filterIsInstance<String>().toSet()
            else -> emptySet()
        }
    }
}

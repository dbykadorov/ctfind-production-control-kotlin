package com.ctfind.productioncontrol.auth.adapter.web

import com.ctfind.productioncontrol.auth.application.AuthenticateUserUseCase
import com.ctfind.productioncontrol.auth.application.AuthenticationResult
import com.ctfind.productioncontrol.auth.application.LoginCommand
import com.ctfind.productioncontrol.auth.application.LogoutUseCase
import com.ctfind.productioncontrol.infrastructure.security.JwtAuthenticationMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
	private val authenticateUser: AuthenticateUserUseCase,
	private val logoutUseCase: LogoutUseCase? = null,
	private val jwtAuthenticationMapper: JwtAuthenticationMapper = JwtAuthenticationMapper(),
) {

	@PostMapping("/login")
	fun login(
		@Valid @RequestBody request: LoginRequest,
		httpRequest: HttpServletRequest,
	): ResponseEntity<Any> {
		val result = authenticateUser.authenticate(
			LoginCommand(
				login = request.login,
				password = request.password,
				requestIp = httpRequest.remoteAddr,
				userAgent = httpRequest.getHeader("User-Agent"),
			),
		)

		return when (result) {
			is AuthenticationResult.Success -> ResponseEntity.ok(result.value.toLoginResponse())
			is AuthenticationResult.ValidationFailed -> ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(AuthErrorResponse("AUTH_VALIDATION_FAILED", result.message))
			AuthenticationResult.Disabled,
			AuthenticationResult.InvalidCredentials,
			-> ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(AuthErrorResponse("AUTH_INVALID_CREDENTIALS", "Invalid login or password"))
			AuthenticationResult.Throttled -> ResponseEntity
				.status(HttpStatus.TOO_MANY_REQUESTS)
				.body(AuthErrorResponse("AUTH_THROTTLED", "Too many attempts. Please try again later"))
		}
	}

	@GetMapping("/me")
	fun me(@AuthenticationPrincipal jwt: Jwt): MeResponse =
		jwtAuthenticationMapper.toMeResponse(jwt)

	@PostMapping("/logout")
	fun logout(
		@AuthenticationPrincipal jwt: Jwt,
		httpRequest: HttpServletRequest,
	): ResponseEntity<Void> {
		logoutUseCase?.logout(
			login = jwt.subject,
			requestIp = httpRequest.remoteAddr,
			userAgent = httpRequest.getHeader("User-Agent"),
		)
		return ResponseEntity.noContent().build()
	}
}

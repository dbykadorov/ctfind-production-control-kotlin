package com.ctfind.productioncontrol.infrastructure.security

import com.ctfind.productioncontrol.auth.application.IssuedToken
import com.ctfind.productioncontrol.auth.application.TokenIssuer
import com.ctfind.productioncontrol.auth.domain.UserAccount
import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@ConfigurationProperties(prefix = "ctfind.auth.jwt")
data class JwtProperties(
	val issuer: String = "ctfind-production-control-contlin-local",
	val secret: String = "local-development-jwt-secret-change-before-production-32b",
	val expiration: Duration = Duration.ofHours(8),
)

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtSecurityConfig {

	@Bean
	fun jwtSecretKey(properties: JwtProperties): SecretKey =
		SecretKeySpec(properties.secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")

	@Bean
	fun jwtEncoder(secretKey: SecretKey): JwtEncoder =
		NimbusJwtEncoder(ImmutableSecret<SecurityContext>(secretKey))

	@Bean
	fun jwtDecoder(secretKey: SecretKey): JwtDecoder =
		NimbusJwtDecoder.withSecretKey(secretKey)
			.macAlgorithm(MacAlgorithm.HS256)
			.build()

	@Bean
	fun clock(): Clock = Clock.systemUTC()
}

@Configuration
class JwtTokenIssuerConfig {

	@Bean
	fun jwtTokenIssuer(
		jwtEncoder: JwtEncoder,
		properties: JwtProperties,
		clock: Clock,
	): TokenIssuer =
		JwtTokenIssuer(jwtEncoder, properties, clock)
}

private class JwtTokenIssuer(
	private val jwtEncoder: JwtEncoder,
	private val properties: JwtProperties,
	private val clock: Clock,
) : TokenIssuer {
	override fun issue(user: UserAccount): IssuedToken {
		val issuedAt = Instant.now(clock)
		val expiresAt = issuedAt.plus(properties.expiration)
		val claims = JwtClaimsSet.builder()
			.issuer(properties.issuer)
			.subject(user.normalizedLogin)
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.claim("userId", user.id.toString())
			.claim("roles", user.roleCodes.toList().sorted())
			.claim("displayName", user.displayName)
			.build()
		val token = jwtEncoder.encode(
			org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(),
				claims,
			),
		)
		return IssuedToken(
			tokenType = "Bearer",
			accessToken = token.tokenValue,
			expiresAt = expiresAt,
		)
	}
}

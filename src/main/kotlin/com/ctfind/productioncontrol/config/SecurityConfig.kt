package com.ctfind.productioncontrol.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http
			.authorizeHttpRequests { requests ->
				requests
					.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
					.anyRequest().authenticated()
			}
			.exceptionHandling { exceptions ->
				exceptions.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
			}
			.sessionManagement { sessions ->
				sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			}
			.csrf { csrf ->
				csrf.disable()
			}

		return http.build()
	}
}

package com.ctfind.productioncontrol.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
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
			.httpBasic(withDefaults())
			.formLogin(withDefaults())

		return http.build()
	}
}

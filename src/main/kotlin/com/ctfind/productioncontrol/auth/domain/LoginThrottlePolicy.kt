package com.ctfind.productioncontrol.auth.domain

import java.time.Clock
import java.time.Duration
import java.time.Instant

data class LoginThrottleBucket(
	val key: String,
	val failureCount: Int,
	val windowStartedAt: Instant,
	val throttledUntil: Instant?,
)

class LoginThrottlePolicy(
	private val maxFailures: Int = 5,
	private val window: Duration = Duration.ofMinutes(5),
	private val throttleDuration: Duration = Duration.ofMinutes(1),
	private val clock: Clock = Clock.systemUTC(),
) {
	fun isThrottled(bucket: LoginThrottleBucket?): Boolean {
		val until = bucket?.throttledUntil ?: return false
		return Instant.now(clock).isBefore(until)
	}

	fun recordFailure(key: String, current: LoginThrottleBucket?): LoginThrottleBucket {
		val now = Instant.now(clock)
		val inWindow = current != null && Duration.between(current.windowStartedAt, now) <= window
		val nextCount = if (inWindow) current.failureCount + 1 else 1
		val startedAt = if (inWindow) current.windowStartedAt else now
		val throttledUntil = if (nextCount >= maxFailures) now.plus(throttleDuration) else null

		return LoginThrottleBucket(
			key = key,
			failureCount = nextCount,
			windowStartedAt = startedAt,
			throttledUntil = throttledUntil,
		)
	}
}

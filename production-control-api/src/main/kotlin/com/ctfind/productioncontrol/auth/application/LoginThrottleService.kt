package com.ctfind.productioncontrol.auth.application

import com.ctfind.productioncontrol.auth.domain.LoginThrottleBucket
import com.ctfind.productioncontrol.auth.domain.LoginThrottlePolicy
import com.ctfind.productioncontrol.auth.domain.normalizeLogin
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class LoginThrottleService(
	maxFailures: Int = 5,
) {
	private val policy = LoginThrottlePolicy(maxFailures = maxFailures)
	private val buckets = ConcurrentHashMap<String, LoginThrottleBucket>()

	fun isThrottled(login: String, requestIp: String?): Boolean =
		policy.isThrottled(buckets[key(login, requestIp)])

	fun recordFailure(login: String, requestIp: String?) {
		val bucketKey = key(login, requestIp)
		buckets.compute(bucketKey) { _, current -> policy.recordFailure(bucketKey, current) }
	}

	fun clear(login: String, requestIp: String?) {
		buckets.remove(key(login, requestIp))
	}

	private fun key(login: String, requestIp: String?): String =
		"${normalizeLogin(login)}|${requestIp ?: "unknown"}"
}

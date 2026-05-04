package com.ctfind.productioncontrol.integration

object ScenarioCoverage {
	const val AUTH_USERS =
		"Covers bootstrap, real login, /api/auth/me, users API admin access, non-admin 403, and unauthenticated 401. Detailed role catalog permutations remain in unit tests."
	const val PRODUCTION_TASKS =
		"Covers order-to-task creation, assignment, status lifecycle, history, executor isolation, stale version conflict, and unauthenticated 401. Detailed transition matrix remains in unit tests."
	const val WAREHOUSE =
		"Covers material receipt, order BOM, consumption, usage totals, insufficient stock, shipped-order lock, and warehouse role checks. Detailed inventory validation matrix remains in unit tests."
	const val ORDERS =
		"Covers create/list/detail/update/status lifecycle, shipped-order lock, non-writer denial, and invalid transition. Detailed order policy matrix remains in unit tests."
	const val NOTIFICATIONS =
		"Covers assignment/status/overdue notification creation, unread count, read state, duplicate overdue guard, and user isolation. Notification rendering remains outside backend integration scope."
	const val AUDIT =
		"Covers real auth/order/production/inventory events in admin audit feed, filters, non-admin 403, and unauthenticated 401. Missing category assertions are recorded as residual risk."
}

package com.ctfind.productioncontrol.production

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Architecture boundary regression tests for Feature 005 (T084).
 *
 * Enforces the hexagonal layout documented in CLAUDE.md and AGENTS.md:
 *   - domain layer has no Spring or JPA dependencies
 *   - application layer owns ports + use cases, no JPA entities or repositories
 *   - adapter/web only adapts HTTP — no JPA repositories or domain mutation
 *   - adapter/persistence is the only place where JPA ↔ domain crosses
 *   - cross-module access from production.application goes through ports,
 *     not direct imports of orders.* / auth.* application or persistence
 */
class ProductionArchitectureTests {

	private val productionRoot = File("src/main/kotlin/com/ctfind/productioncontrol/production")

	@Test
	fun `production domain has no spring or jpa dependencies`() {
		val offenders = filesUnder(productionRoot.resolve("domain")).filter { file ->
			file.readText().lines().any { line ->
				val import = line.trim()
				import.startsWith("import org.springframework")
					|| import.startsWith("import jakarta.persistence")
					|| import.startsWith("import javax.persistence")
			}
		}
		assertEmpty(offenders, "production/domain must not depend on Spring or JPA")
	}

	@Test
	fun `production application layer does not import jpa entities or repositories`() {
		val offenders = filesUnder(productionRoot.resolve("application")).filter { file ->
			file.readText().lines().any { line ->
				val trimmed = line.trim()
				trimmed.startsWith("import jakarta.persistence")
					|| trimmed.startsWith("import javax.persistence")
					|| trimmed.contains(".adapter.persistence.")
					|| trimmed.contains(".adapter.web.")
			}
		}
		assertEmpty(offenders, "production/application must not depend on adapters or JPA")
	}

	@Test
	fun `production web controllers do not access JPA repositories or persistence directly`() {
		val webDir = productionRoot.resolve("adapter/web")
		val offenders = filesUnder(webDir).filter { file ->
			file.readText().lines().any { line ->
				val trimmed = line.trim()
				trimmed.startsWith("import org.springframework.data.jpa")
					|| trimmed.startsWith("import org.springframework.data.repository")
					|| trimmed.startsWith("import jakarta.persistence")
					|| trimmed.contains(".adapter.persistence.")
			}
		}
		assertEmpty(offenders, "production/adapter/web must not access persistence layer or JPA repositories")
	}

	@Test
	fun `production persistence adapters do not import controllers or web DTOs`() {
		val persistenceDir = productionRoot.resolve("adapter/persistence")
		val offenders = filesUnder(persistenceDir).filter { file ->
			file.readText().lines().any { line ->
				val trimmed = line.trim()
				trimmed.contains(".adapter.web.")
			}
		}
		assertEmpty(offenders, "production/adapter/persistence must not import web layer types")
	}

	@Test
	fun `production application does not import other modules adapter packages`() {
		val applicationDir = productionRoot.resolve("application")
		val offenders = filesUnder(applicationDir).filter { file ->
			file.readText().lines().any { line ->
				val trimmed = line.trim()
				(trimmed.startsWith("import com.ctfind.productioncontrol.orders.adapter")
					|| trimmed.startsWith("import com.ctfind.productioncontrol.auth.adapter"))
			}
		}
		assertEmpty(
			offenders,
			"production/application must consume cross-module data through ports, not adapter imports",
		)
	}

	@Test
	fun `production controller delegates writes only through application use cases`() {
		val controller = productionRoot.resolve("adapter/web/ProductionTaskController.kt")
		val text = controller.readText()
		// Controller body must not call repository.save / repository.findAll directly.
		assertTrue(
			!text.contains(".save(") || text.contains("query.detail") || text.contains("toDetailResponse"),
			"Controller appears to call repository.save() directly — writes must go through use cases",
		)
		// And it must wire the application use cases via constructor.
		listOf(
			"ProductionTaskQueryUseCase",
			"CreateProductionTasksFromOrderUseCase",
			"AssignProductionTaskUseCase",
			"ChangeProductionTaskStatusUseCase",
			"ProductionTaskAssigneeQueryUseCase",
		).forEach { dep ->
			assertTrue(
				text.contains(dep),
				"Controller should depend on $dep so HTTP only adapts to use cases",
			)
		}
	}

	private fun filesUnder(root: File): List<File> {
		if (!root.exists()) return emptyList()
		return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
	}

	private fun assertEmpty(offenders: List<File>, message: String) {
		assertTrue(
			offenders.isEmpty(),
			"$message — offending files: ${offenders.joinToString { it.relativeTo(File("src/main/kotlin/com/ctfind/productioncontrol")).path }}",
		)
	}
}

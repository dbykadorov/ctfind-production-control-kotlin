plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
}

group = "com.ctfind"
version = "0.0.1-SNAPSHOT"
description = "CTfind Production Control Contlin"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

val testcontainersVersion = "1.20.6"

val springIntegrationTestSourceSet = sourceSets.create("springIntegrationTest") {
	kotlin.srcDir("src/integrationTest/kotlin")
	resources.srcDir("src/integrationTest/resources")
	compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
	runtimeClasspath += output + compileClasspath
}

configurations.named(springIntegrationTestSourceSet.implementationConfigurationName) {
	extendsFrom(configurations.testImplementation.get())
}

configurations.named(springIntegrationTestSourceSet.runtimeOnlyConfigurationName) {
	extendsFrom(configurations.testRuntimeOnly.get())
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.mockk:mockk:1.13.13")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	"springIntegrationTestImplementation"("org.springframework.boot:spring-boot-starter-actuator-test")
	"springIntegrationTestImplementation"("org.springframework.boot:spring-boot-starter-data-jpa-test")
	"springIntegrationTestImplementation"("org.springframework.boot:spring-boot-starter-flyway-test")
	"springIntegrationTestImplementation"("org.springframework.boot:spring-boot-starter-security-test")
	"springIntegrationTestImplementation"("org.springframework.boot:spring-boot-starter-validation-test")
	"springIntegrationTestImplementation"("org.springframework.boot:spring-boot-starter-webmvc-test")
	"springIntegrationTestImplementation"("org.jetbrains.kotlin:kotlin-test-junit5")
	"springIntegrationTestImplementation"("org.testcontainers:junit-jupiter:$testcontainersVersion")
	"springIntegrationTestImplementation"("org.testcontainers:postgresql:$testcontainersVersion")
	"springIntegrationTestRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

val springIntegrationTest by tasks.registering(Test::class) {
	description = "Runs Docker-backed Spring integration scenario tests."
	group = LifecycleBasePlugin.VERIFICATION_GROUP
	testClassesDirs = springIntegrationTestSourceSet.output.classesDirs
	classpath = springIntegrationTestSourceSet.runtimeClasspath
	shouldRunAfter(tasks.test)
	useJUnitPlatform()
}

tasks.check {
	dependsOn(springIntegrationTest)
}

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.5.6"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.5.31"
	kotlin("plugin.spring") version "1.5.31"
	kotlin("plugin.jpa") version "1.5.31"
	kotlin("plugin.allopen") version "1.5.31"
	kotlin("kapt") version "1.5.31"
}

allOpen {
	annotation("javax.persistence.Entity")
	annotation("javax.persistence.Embeddable")
	annotation("javax.persistence.MappedSuperclass")
}

group = "io.github.bayang"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.session:spring-session-core")
	implementation("com.github.gotson:spring-session-caffeine:1.0.3")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	kapt("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.xerial:sqlite-jdbc")
	implementation("org.liquibase:liquibase-core")
	val exposedVersion = "0.36.1"
	implementation ("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
//	implementation("org.nuvito.spring.data:sqlite-dialect:1.0-SNAPSHOT")
	implementation("io.github.microutils:kotlin-logging-jvm:2.0.10")
	implementation("com.github.slugify:slugify:2.4")
	implementation("commons-io:commons-io:2.11.0")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("spring.profiles.active", "test")
}

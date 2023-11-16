import com.github.gradle.node.npm.task.NpmTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.4"
    val kotlinVersion = "1.8.22"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("com.github.node-gradle.node") version "6.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.0"
}

kotlin {
    jvmToolchain(17)
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("0.50.0")
    outputToConsole.set(true)
    coloredOutput.set(true)
    disabledRules.set(setOf("no-wildcard-imports", "parameter-list-wrapping"))
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
}

group = "io.github.bayang"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.session:spring-session-core")
    implementation("com.github.gotson:spring-session-caffeine:1.0.3")
    implementation("org.springframework.security:spring-security-ldap")
    // implementation("com.unboundid:unboundid-ldapsdk:6.0.5")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("com.github.ben-manes.caffeine:caffeine")

    implementation("com.fasterxml.staxmate:staxmate:2.4.0")
    implementation("com.fasterxml.woodstox:woodstox-core:6.5.0")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.xerial:sqlite-jdbc")
    implementation("org.liquibase:liquibase-core")
    val exposedVersion = "0.44.1"
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
// 	implementation("org.nuvito.spring.data:sqlite-dialect:1.0-SNAPSHOT")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("com.github.slugify:slugify:3.0.2")
    implementation("commons-io:commons-io:2.11.0")
    implementation("commons-validator:commons-validator:1.7")
    implementation("org.jsoup:jsoup:1.15.4")
    implementation("net.coobird:thumbnailator:0.4.19")

    implementation("org.apache.commons:commons-csv:1.10.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("io.mockk:mockk:1.13.4")
    testImplementation("com.ninja-squad:springmockk:3.1.2")
    testImplementation("io.projectreactor:reactor-test")

    val springdocVersion = "1.7.0"
    implementation("org.springdoc:springdoc-openapi-ui:$springdocVersion")
    implementation("org.springdoc:springdoc-openapi-security:$springdocVersion")
    implementation("org.springdoc:springdoc-openapi-kotlin:$springdocVersion")
    implementation("org.springdoc:springdoc-openapi-data-rest:$springdocVersion")

    val luceneVersion = "9.7.0"
    implementation("org.apache.lucene:lucene-core:$luceneVersion")
    implementation("org.apache.lucene:lucene-analysis-common:$luceneVersion")
    implementation("org.apache.lucene:lucene-queryparser:$luceneVersion")
    implementation("org.apache.lucene:lucene-backward-codecs:$luceneVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf(
            "-Xjsr305=strict",
            "-opt-in=kotlin.time.ExperimentalTime",
        )
        jvmTarget = "17"
    }
}

tasks.getByName<BootJar>("bootJar") {
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}

springBoot {
    buildInfo()
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

// unpack Spring Boot's fat jar for better Docker image layering
tasks.register<JavaExec>("unpack") {
    dependsOn(tasks.bootJar)
    classpath = files(tasks.bootJar)
    jvmArgs = listOf("-Djarmode=layertools")
    args = "extract --destination ${layout.buildDirectory.get()}/dependency".split(" ")
    doFirst {
        delete("${layout.buildDirectory.get()}/dependency")
    }
}

node {
    nodeProjectDir.set(file("${project.projectDir}/src/jelu-ui"))
    version.set("16.13.2")
    download.set(true)
}

val buildTaskUsingNpm = tasks.register<NpmTask>("npmBuild") {
    dependsOn(tasks.npmInstall)
    npmCommand.set(listOf("run", "build"))
    args.set(listOf("--", "--out-dir", "${layout.buildDirectory.get()}/npm-output"))
    inputs.dir("src")
    outputs.dir("${layout.buildDirectory.get()}/npm-output")
}

tasks.register<Sync>("copyWebDist") {
    dependsOn("npmBuild")
    from("${layout.buildDirectory.get()}/npm-output")
    into("$projectDir/src/main/resources/public/")
}

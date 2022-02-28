import com.github.gradle.node.npm.task.NpmTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "2.6.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"
    kotlin("plugin.jpa") version "1.6.10"
    kotlin("plugin.allopen") version "1.6.10"
    kotlin("kapt") version "1.6.10"
    id("com.github.node-gradle.node") version "3.2.1"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    outputToConsole.set(true)
    coloredOutput.set(true)
    disabledRules.set(setOf("no-wildcard-imports"))
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
}

group = "io.github.bayang"
java.sourceCompatibility = JavaVersion.VERSION_11

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
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("com.github.ben-manes.caffeine:caffeine")

    implementation("com.fasterxml.staxmate:staxmate:2.4.0")
    implementation("com.fasterxml.woodstox:woodstox-core:6.2.8")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.xerial:sqlite-jdbc")
    implementation("org.liquibase:liquibase-core")
    val exposedVersion = "0.37.3"
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
// 	implementation("org.nuvito.spring.data:sqlite-dialect:1.0-SNAPSHOT")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.21")
    implementation("com.github.slugify:slugify:2.5")
    implementation("commons-io:commons-io:2.11.0")
    implementation("commons-validator:commons-validator:1.7")
    implementation("org.jsoup:jsoup:1.14.3")

    implementation("org.apache.commons:commons-csv:1.9.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")

    val springdocVersion = "1.6.6"
    implementation("org.springdoc:springdoc-openapi-ui:$springdocVersion")
    implementation("org.springdoc:springdoc-openapi-security:$springdocVersion")
    implementation("org.springdoc:springdoc-openapi-kotlin:$springdocVersion")
    implementation("org.springdoc:springdoc-openapi-data-rest:$springdocVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.getByName<BootJar>("bootJar") {
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

// unpack Spring Boot's fat jar for better Docker image layering
tasks.register<JavaExec>("unpack") {
    dependsOn(tasks.bootJar)
    classpath = files(tasks.bootJar)
    jvmArgs = listOf("-Djarmode=layertools")
    args = "extract --destination $buildDir/dependency".split(" ")
    doFirst {
        delete("$buildDir/dependency")
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
    args.set(listOf("--", "--out-dir", "$buildDir/npm-output"))
    inputs.dir("src")
    outputs.dir("$buildDir/npm-output")
}

tasks.register<Sync>("copyWebDist") {
    dependsOn("npmBuild")
    from("$buildDir/npm-output")
    into("$projectDir/src/main/resources/public/")
}

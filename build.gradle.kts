import com.github.gradle.node.npm.task.NpmTask
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.7"
    val kotlinVersion = "2.2.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("com.github.node-gradle.node") version "7.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        freeCompilerArgs =
            listOf(
                "-Xjsr305=strict",
                "-Xemit-jvm-type-annotations",
                "-opt-in=kotlin.time.ExperimentalTime",
                "-Xannotation-default-target=param-property",
            )
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    outputToConsole.set(true)
    coloredOutput.set(true)
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
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.data:spring-data-jdbc") // required since exposed 0.51.0
    implementation("org.springframework.session:spring-session-core")
    implementation("org.springframework.session:spring-session-jdbc")
    implementation("org.springframework.security:spring-security-ldap")
    // implementation("com.unboundid:unboundid-ldapsdk:6.0.5")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.apache.httpcomponents.client5:httpclient5")

    implementation("com.github.ben-manes.caffeine:caffeine")

    implementation("com.fasterxml.staxmate:staxmate:2.4.1")
    implementation("com.fasterxml.woodstox:woodstox-core:7.1.1")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.xerial:sqlite-jdbc")
    implementation("org.liquibase:liquibase-core")
    val exposedVersion = "0.61.0"
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
// 	implementation("org.nuvito.spring.data:sqlite-dialect:1.0-SNAPSHOT")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("com.github.slugify:slugify:3.0.7")
    implementation("commons-io:commons-io:2.21.0")
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("commons-validator:commons-validator:1.10.1")
    implementation("org.jsoup:jsoup:1.21.2")
    implementation("net.coobird:thumbnailator:0.4.21")

    implementation("org.apache.commons:commons-csv:1.14.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("io.mockk:mockk:1.14.7")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("io.projectreactor:reactor-test")

    val springdocVersion = "2.8.14"
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    val luceneVersion = "9.12.3"
    implementation("org.apache.lucene:lucene-core:$luceneVersion")
    implementation("org.apache.lucene:lucene-analysis-common:$luceneVersion")
    implementation("org.apache.lucene:lucene-queryparser:$luceneVersion")
    implementation("org.apache.lucene:lucene-backward-codecs:$luceneVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}

// tasks.withType<KotlinCompile> {
//     kotlinOptions {
//         freeCompilerArgs = listOf(
//             "-Xjsr305=strict",
//             "-opt-in=kotlin.time.ExperimentalTime",
//         )
//         jvmTarget = "17"
//     }
// }

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
    version.set("20.19.6")
    npmInstallCommand.set("ci")
    download.set(true)
}

val buildTaskUsingNpm =
    tasks.register<NpmTask>("npmBuild") {
        npmCommand.set(listOf("run", "install-build"))
        args.set(listOf("--", "--out-dir", "${layout.buildDirectory.get()}/npm-output"))
        inputs.dir("src")
        outputs.dir("${layout.buildDirectory.get()}/npm-output")
    }

tasks.register<Sync>("copyWebDist") {
    dependsOn("npmBuild")
    from("${layout.buildDirectory.get()}/npm-output")
    into("$projectDir/src/main/resources/public/")
}

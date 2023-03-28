@file:Suppress("PropertyName")

import com.google.protobuf.gradle.id
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version = "2.2.4"
val kotlin_version = "1.8.10"
val logback_version = "1.4.6"
val guava_version = "31.1-jre"
val jsonpath_version = "2.8.0"
val kxs_version = "1.5.0"
val proto_version = "3.22.2"
val protokt_version = "0.10.2"
val grpc_version = "1.3.0"
val grpcnetty_version = "1.54.0"
val kotest_version = "5.5.5"
val kotest_allure_version = "1.2.0"
val kotest_pitest_version = "1.1.0"
val knbt_version = "0.11.3"
val mordant_version = "2.0.0-beta12"

plugins {
    application
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    kotlin("kapt") version "1.8.10"
    id("io.qameta.allure") version "2.11.2"
    id("info.solidsoft.pitest") version "1.9.11"
    id("com.toasttab.protokt") version "0.10.2"
    id("idea")
}

group = "msw.server"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    // grpc
    implementation("com.toasttab.protokt:protokt-runtime-grpc:$protokt_version")
    implementation("com.google.protobuf:protobuf-java:$proto_version")
    implementation("io.grpc:grpc-kotlin-stub:$grpc_version")
    implementation("io.grpc:grpc-stub:$grpcnetty_version")
    implementation("io.grpc:protoc-gen-grpc-kotlin:$grpc_version")
    runtimeOnly("io.grpc:grpc-netty-shaded:$grpcnetty_version")

    // ktor
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")

    // additionals
    implementation("com.google.guava:guava:$guava_version")
    implementation("com.jayway.jsonpath:json-path:$jsonpath_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kxs_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:$kxs_version")
    implementation("net.benwoodworth.knbt:knbt:$knbt_version")
    implementation("com.github.ajalt.mordant:mordant:$mordant_version")

    // testing
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotest_version")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotest_version")
    testImplementation("io.kotest:kotest-property-jvm:$kotest_version")
    testImplementation("io.kotest.extensions:kotest-extensions-allure:$kotest_allure_version")
    testImplementation("io.kotest.extensions:kotest-extensions-pitest:$kotest_pitest_version")
}

application {
    mainClass.set("msw.server.core.MainKt")
}

protokt {
    generateGrpc = true
}

protobuf {
    plugins {
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpc_version:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpckt")
            }
        }
    }
}

tasks.withType<JavaCompile>().all {
    enabled = false
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

// test scripting
tasks.withType<Test> {
    useJUnitPlatform()
}

allure {
    adapter.autoconfigure.set(false)
    version.set("2.21.0")
}

configure<PitestPluginExtension> {
    testPlugin.set("Kotest")
    targetClasses.set(listOf("msw.server.*"))
}

val testDir1Path = "/src/test/resources/DirectoryTest"
val testDir2Path = "/src/test/resources/Create"
tasks.register("preTest") {
    doLast {
        mkdir(testDir1Path)
        file("$testDir1Path/empty.json").apply { createNewFile() }.writeText("")
        file("$testDir1Path/json00.json").apply { createNewFile() }.writeText("{ \"s\": \"s\", \"i\": 0, \"b\": true}")
        file("$testDir1Path/json01.json").apply { createNewFile() }.writeText("{}")
        file("$testDir1Path/json02.json").apply { createNewFile() }.writeText("{}")
        file("$testDir1Path/json03.json").apply { createNewFile() }.writeText("{}")
        file("$testDir1Path/json04.json").apply { createNewFile() }.writeText("{}")
        file("$testDir1Path/json05.json").apply { createNewFile() }.writeText("{}")
        file("$testDir1Path/json06.json").apply { createNewFile() }.writeText("{}")
    }
}

tasks.register<Delete>("postTest") {
    for (task in tasks.withType<Test>()) {
        mustRunAfter(task)
    }
    delete(testDir1Path)
    delete(testDir2Path)
    delete("/src/test/resources/NonExistent") // might be created by :pitest
}

tasks.withType<Test>().configureEach {
    dependsOn("preTest")
}
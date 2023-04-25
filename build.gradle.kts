@file:Suppress("PropertyName")

import com.google.protobuf.gradle.id
import info.solidsoft.gradle.pitest.PitestPluginExtension
import kotlin.math.min
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version = "2.2.4"
val kotlin_version = "1.8.20"
val logback_version = "1.4.6"
val guava_version = "31.1-jre"
val jsonpath_version = "2.8.0"
val kxs_version = "1.5.0"
val proto_version = "3.22.3"
val protokt_version = "0.10.2"
val grpc_version = "1.3.0"
val grpcnetty_version = "1.54.0"
val kotest_version = "5.6.1"
val kotest_allure_version = "1.2.0"
val kotest_pitest_version = "1.2.0"
val knbt_version = "0.11.3"
val mordant_version = "2.0.0-beta12"
val mockk_version = "1.13.4"

plugins {
    application
    kotlin("jvm") version "1.8.20"
    kotlin("plugin.serialization") version "1.8.20"
    kotlin("kapt") version "1.8.20"
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
    testImplementation("io.mockk:mockk:$mockk_version")
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
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xcontext-receivers")
    }
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
    // only TIMED_OUT if bigger value is used
    threads.set(min(3, Runtime.getRuntime().availableProcessors()))
    targetClasses.set(listOf("msw.server.*"))
}
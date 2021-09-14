import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val guava_version = "30.1.1-jre"
val jsonpath_version = "2.6.0"
val kxs_version = "1.2.2"
val arrow_version = "0.13.2"
val proto_version = "3.17.3"
val protokt_version = "0.6.4"
val grpc_version = "1.1.0"
val grpcnetty_version = "1.40.1"
val kotest_version = "5.0.0.M1"
val kotest_allure_version = "1.0.2"
val kotest_pitest_version = "1.0.1"
val knbt_version = "0.9.2"

plugins {
    application
    kotlin("jvm") version "1.5.30"
    kotlin("plugin.serialization") version "1.5.30"
    kotlin("kapt") version "1.5.30"
    id("io.qameta.allure") version "2.8.1"
    id("info.solidsoft.pitest") version "1.6.0"
    id("com.toasttab.protokt") version "0.6.4"
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
    runtimeOnly("io.grpc:grpc-netty-shaded:$grpcnetty_version")

    // ktor
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-apache:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // additionals
    implementation("com.google.guava:guava:$guava_version")
    implementation("com.jayway.jsonpath:json-path:$jsonpath_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kxs_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:$kxs_version")
    implementation("net.benwoodworth.knbt:knbt:$knbt_version")

    // testing
    testImplementation("io.kotest:kotest-runner-junit5:$kotest_version")
    testImplementation("io.kotest:kotest-assertions-core:$kotest_version")
    testImplementation("io.kotest:kotest-property:$kotest_version")
    testImplementation("io.kotest.extensions:kotest-extensions-allure:$kotest_allure_version")
    testImplementation("io.kotest.extensions:kotest-extensions-pitest:$kotest_pitest_version")
}

protokt {
    generateGrpc = true
}

tasks.withType<JavaCompile>().all {
    enabled = false
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
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
    autoconfigure = false
    version = "2.14.0"
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
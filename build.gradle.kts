import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val guava_version = "29.0-jre"
val jsonpath_version = "2.4.0"
val kxs_version = "1.0.0"
val arrow_version = "0.11.0"
val kotest_version = "4.3.1"

plugins {
    application
    kotlin("jvm") version "1.4.20"
    kotlin("plugin.serialization") version "1.4.20"
    kotlin("kapt") version "1.4.20"
    id("io.qameta.allure") version "2.8.1"
    id("info.solidsoft.pitest") version "1.5.1"
}

group = "msw.server"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.tomcat.EngineMain")
}

repositories {
    jcenter()
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
    maven { url = uri("https://dl.bintray.com/arrow-kt/arrow-kt/") }
}

dependencies {
    // stdlib
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")

    // ktor
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-server-tomcat:$ktor_version")
    implementation("io.ktor:ktor-client-apache:$ktor_version")
    implementation("io.ktor:ktor-network:$ktor_version")
    implementation("io.ktor:ktor-network-tls:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")

    // additionals
    implementation("com.google.guava:guava:$guava_version")
    implementation("com.jayway.jsonpath:json-path:$jsonpath_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kxs_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:$kxs_version")
    implementation("io.arrow-kt:arrow-core:$arrow_version")
    implementation("io.arrow-kt:arrow-syntax:$arrow_version")
    kapt("io.arrow-kt:arrow-meta:$arrow_version")

    // testing
    testImplementation("io.kotest:kotest-runner-junit5:$kotest_version")
    testImplementation("io.kotest:kotest-assertions-core:$kotest_version")
    testImplementation("io.kotest:kotest-assertions-arrow:$kotest_version")
    testImplementation("io.kotest:kotest-property:$kotest_version")
    testImplementation("io.kotest:kotest-extensions-allure:$kotest_version")
    testImplementation("io.kotest:kotest-plugins-pitest:$kotest_version")
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// test scripting
tasks.withType<Test> {
    useJUnitPlatform()
}

allure {
    autoconfigure = false
    version = "2.13.6"
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
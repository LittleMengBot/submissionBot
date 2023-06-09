plugins {
    kotlin("jvm") version "1.8.22"
    application
}

group = "org.meng"
version = "1.0.1-beta-patch-3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")
    // Logger
    implementation(group = "io.github.microutils", name = "kotlin-logging-jvm", version = "3.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    // Bot Api
    implementation("dev.inmo:tgbotapi-jvm:8.1.0")
    // DB
    implementation("io.github.crackthecodeabhi:kreds:0.8.1")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}

tasks.jar {
    manifest {
        attributes(mapOf("Main-Class" to "MainKt"))
    }
    from(
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    )
    val sourcesMain = sourceSets.main.get()
    from(sourcesMain.output)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveFileName.set("release.jar")
}

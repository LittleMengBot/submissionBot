plugins {
    kotlin("jvm") version "1.8.21"
    application
}

group = "org.meng"
version = "1.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://git.inmo.dev/api/packages/InsanusMokrassar/maven")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.20")
    // Logger
    implementation(group = "io.github.microutils", name = "kotlin-logging-jvm", version = "3.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    // Bot Api
    implementation("dev.inmo:tgbotapi-jvm:7.1.2-branch_7.1.2-build1639")
    // DB
    implementation("com.github.jasync-sql:jasync-mysql:2.1.24")
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

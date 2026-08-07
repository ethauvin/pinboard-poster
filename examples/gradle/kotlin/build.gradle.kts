import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("application")
    id("io.github.ben-manes.versions") version "0.60.0"
    kotlin("jvm") version "2.4.10"
}

defaultTasks(ApplicationPlugin.TASK_RUN_NAME)

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
}

dependencies {
    implementation("net.thauvin.erik:pinboard-poster:1.3.0-SNAPSHOT")
}

application {
    mainClass.set("net.thauvin.erik.pinboard.samples.KotlinExampleKt")
}

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("application")
    id("com.github.ben-manes.versions") version "0.53.0"
    kotlin("jvm") version "2.2.21"
}

defaultTasks(ApplicationPlugin.TASK_RUN_NAME)

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
}

dependencies {
    implementation("net.thauvin.erik:pinboard-poster:1.2.1-SNAPSHOT")
}

application {
    mainClass.set("net.thauvin.erik.pinboard.samples.KotlinExampleKt")
}

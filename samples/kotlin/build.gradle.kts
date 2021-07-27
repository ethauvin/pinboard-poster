plugins {
    id("application")
    kotlin("jvm") version "1.5.21"
}

// ./gradlew run

defaultTasks(ApplicationPlugin.TASK_RUN_NAME)

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("net.thauvin.erik:pinboard-poster:1.0.4-SNAPSHOT")
}

application {
    mainClass.set("net.thauvin.erik.pinboard.samples.KotlinExampleKt")
}

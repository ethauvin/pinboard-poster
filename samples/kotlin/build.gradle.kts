plugins {
    application
    kotlin("jvm") version "1.5.0"
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
    mainClassName = "net.thauvin.erik.pinboard.samples.KotlinExampleKt"
}

plugins {
    application
    kotlin("jvm") version "1.4.31"
}

// ./gradlew run

defaultTasks(ApplicationPlugin.TASK_RUN_NAME)

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compile("net.thauvin.erik:pinboard-poster:1.0.2")
}

application {
    mainClassName = "net.thauvin.erik.pinboard.samples.KotlinExampleKt"
}

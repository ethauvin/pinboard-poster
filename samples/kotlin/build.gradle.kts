plugins {
    application
    kotlin("jvm") version "1.4.0"
}

// .gradlew run

defaultTasks(ApplicationPlugin.TASK_RUN_NAME)

dependencies {
    compile("net.thauvin.erik:pinboard-poster:1.0.1")
}

application {
    mainClassName = "net.thauvin.erik.pinboard.samples.KotlinExampleKt"
}

repositories {
    mavenLocal()
    jcenter()
}

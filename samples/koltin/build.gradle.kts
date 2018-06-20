import org.gradle.api.tasks.wrapper.Wrapper.DistributionType

plugins {
    application
    kotlin("jvm") version "1.2.50"
}

defaultTasks(ApplicationPlugin.TASK_RUN_NAME)

dependencies {
    compile("net.thauvin.erik:pinboard-poster:1.0.0")
}

application {
    mainClassName = "net.thauvin.erik.pinboard.samples.KotlinExampleKt"
}

repositories {
    mavenLocal()
    jcenter()
}
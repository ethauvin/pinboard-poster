import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.publish.maven.MavenPom
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    application
    kotlin("jvm") version "1.2.50"
    java
    `maven-publish`
    id("com.github.ben-manes.versions") version "0.20.0"
    id("com.jfrog.bintray") version "1.8.2"
    id("org.jetbrains.dokka") version "0.9.17"
}

group = "net.thauvin.erik"
version = "0.9.3"
description = "Pinboard Poster for Kotlin/Java"

val mavenUrl = "https://github.com/ethauvin/pinboard-poster"
val deployDir = "deploy"

dependencies {
    compile(kotlin("stdlib"))
    compile("com.squareup.okhttp3:okhttp:3.10.0")
    testCompile("org.testng:testng:6.14.3")
}

repositories {
    jcenter()
}

application {
    mainClassName = "net.thauvin.erik.pinboard.PinboardPosterKt"
}

tasks {
    withType(Test::class.java).all {
        useTestNG()
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<Javadoc> {
        options {
            header = project.name
            encoding = "UTF-8"
        }
    }

    val sourcesJar by creating(Jar::class) {
        classifier = "sources"
        from(java.sourceSets["main"].allSource)
    }

    val dokka by getting(DokkaTask::class) {
        dependsOn(java.sourceSets["main"].classesTaskName)
        outputFormat = "html"
        outputDirectory = "$buildDir/javadoc"
        // See https://github.com/Kotlin/dokka/issues/196
        externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
            url = URL("https://docs.oracle.com/javase/8/docs/api/")
        })

        includeNonPublic = false
    }

    val javadocJar by creating(Jar::class) {
        dependsOn(dokka)
        from(dokka.outputDirectory)
        classifier = "javadoc"
        description = "Assembles a JAR of the generated Javadoc"
        group = JavaBasePlugin.DOCUMENTATION_GROUP
    }

    "runJava"(JavaExec::class) {
        description = "Run this project as a Java application."
        group = ApplicationPlugin.APPLICATION_GROUP
        main = "net.thauvin.erik.pinboard.example.JavaExample"
        classpath = java.sourceSets["main"].runtimeClasspath
    }

    "assemble" {
        dependsOn(sourcesJar, javadocJar)
    }

    val copyToDeploy by creating(Copy::class) {
        from(configurations.runtime)
        from(tasks["jar"])
        into(deployDir)
    }

    "deploy" {
        description = "Copies all needed files to the $deployDir directory."
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        dependsOn("build")
        outputs.dir(deployDir)
        inputs.files(copyToDeploy)
        mustRunAfter("clean")
    }

    val gitTag by creating(Exec::class) {
        description = "Tags the local repository with version ${project.version}"
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        commandLine("git", "tag", "-a", project.version, "-m", "Version ${project.version}")
    }


    val publicationName = "mavenJava"
    publishing {
        (publications) {
            publicationName(MavenPublication::class) {
                from(components["java"])
                artifact(sourcesJar)
                artifact(javadocJar)
                pom.withXml {
                    asNode().let { root ->
                        root.appendNode("name", project.name)
                        root.appendNode("description", project.description)
                        root.appendNode("url", mavenUrl)

                        root.appendNode("licenses").appendNode("license").apply {
                            appendNode("name", "BSD 3-Clause")
                            appendNode("url", "https://opensource.org/licenses/BSD-3-Clause")
                        }

                        root.appendNode("developers").appendNode("developer").apply {
                            appendNode("id", "ethauvin")
                            appendNode("name", "Erik C. Thauvin")
                            appendNode("email", "erik@thauvin.net")
                        }

                        root.appendNode("scm").apply {
                            appendNode("connection", "$mavenUrl.git")
                            appendNode("developerConnection", "git@github.com:ethauvin/pinboard-poster.git")
                            appendNode("url", mavenUrl)
                        }
                    }
                }
            }
        }
    }

    val generatePom by creating {
        description = "Generates pom.xml for snyk."
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        dependsOn("generatePomFileForMavenJavaPublication")
        val pom = File("build/publications/$publicationName/pom-default.xml")
        if (pom.exists()) {
            pom.copyTo(File("pom.xml"), true)
        }
    }

    fun findProperty(s: String) = project.findProperty(s) as String?
    bintray {
        user = findProperty("bintrayUser")
        key = findProperty("bintrayApiKey")
        publish = true
        setPublications(publicationName)
        pkg.apply {
            repo = "maven"
            name = project.name
            desc = description
            websiteUrl = mavenUrl
            issueTrackerUrl = "$mavenUrl/issues"
            githubRepo = "ethauvin/pinboard-poster"
            vcsUrl = mavenUrl
            setLabels("kotlin", "java", "pinboard", "poster", "bookmarks")
            version.apply {
                name = project.version as String
                desc = description
                vcsTag = project.version as String
                gpg.apply {
                    sign = true
                }
            }
        }
    }

    val bintrayUpload by getting {
        dependsOn(gitTag)
    }

    "release" {
        dependsOn(generatePom, gitTag, bintrayUpload)
    }
}
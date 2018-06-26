import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.publish.maven.MavenPom
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.LinkMapping
import groovy.lang.Closure
import java.io.FileInputStream
import java.net.URL
import java.util.Properties


plugins {
    kotlin("jvm") version "1.2.50"
    `build-scan`
    java
    `maven-publish`
    id("com.github.ben-manes.versions") version "0.20.0"
    id("com.jfrog.bintray") version "1.8.2"
    id("org.jetbrains.dokka") version "0.9.17"
}

group = "net.thauvin.erik"
version = "1.0.0"
description = "Pinboard Poster for Kotlin/Java"

val gitHub = "ethauvin/$name"
val mavenUrl = "https://github.com/$gitHub"
val deployDir = "deploy"
var release = false

// Load local.properties
File("local.properties").apply {
    if (exists()) {
        FileInputStream(this).use { fis ->
            Properties().apply {
                load(fis)
                forEach { (k, v) ->
                    extra[k as String] = v
                }
            }
        }
    }
}

repositories {
    jcenter()
}

dependencies {
    compile(kotlin("stdlib"))
    compile("com.squareup.okhttp3:okhttp:3.10.0")
    testCompile("org.testng:testng:6.14.3")
}

tasks {
    withType(Test::class.java).all {
        useTestNG()
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
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
            packageListUrl = URL("https://docs.oracle.com/javase/8/docs/api/package-list")
        })
        val mapping = LinkMapping().apply {
            dir = project.rootDir.toPath().resolve("src/main/kotlin").toFile().path
            url = "https://github.com/ethauvin/pinboard-poster/blob/${project.version}/src/main/kotlin"
            suffix = "#L"
        }
        linkMappings = arrayListOf(mapping)

        includeNonPublic = false
    }

    val javadocJar by creating(Jar::class) {
        dependsOn(dokka)
        from(dokka.outputDirectory)
        classifier = "javadoc"
        description = "Assembles a JAR of the generated Javadoc"
        group = JavaBasePlugin.DOCUMENTATION_GROUP
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

    val gitRefreshIndex by creating(Exec::class) {
        description = "Refreshes the git index."
        commandLine("git", "update-index", "--refresh").isIgnoreExitValue = true
    }

    val gitIsDirty by creating(Exec::class) {
        description = "Fails if git has uncommitted changes."
        group = "verification"
        dependsOn(gitRefreshIndex)
        commandLine("git", "diff-index", "--quiet", "HEAD", "--")
    }

    val gitTag by creating(Exec::class) {
        description = "Tags the local repository with version ${project.version}"
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        dependsOn(gitIsDirty)
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
                    asNode().apply {
                        appendNode("name", project.name)
                        appendNode("description", project.description)
                        appendNode("url", mavenUrl)

                        appendNode("licenses").appendNode("license").apply {
                            appendNode("name", "BSD 3-Clause")
                            appendNode("url", "https://opensource.org/licenses/BSD-3-Clause")
                        }

                        appendNode("developers").appendNode("developer").apply {
                            appendNode("id", "ethauvin")
                            appendNode("name", "Erik C. Thauvin")
                            appendNode("email", "erik@thauvin.net")
                        }

                        appendNode("scm").apply {
                            appendNode("connection", "scm:git:$mavenUrl.git")
                            appendNode("developerConnection", "scm:git:git@github.com:$gitHub.git")
                            appendNode("url", mavenUrl)
                        }

                        appendNode("issueManagement").apply {
                            appendNode("system", "GitHub")
                            appendNode("url", "$mavenUrl/issues")
                        }
                    }
                }
            }
        }
    }

    val generatePomFileForMavenJavaPublication by getting(GenerateMavenPom::class) {
        destination = file("$projectDir/pom.xml")
    }

    val bintrayUpload by getting(BintrayUploadTask::class) {
        dependsOn(generatePomFileForMavenJavaPublication, gitTag)
    }

    fun findProperty(s: String) = project.findProperty(s) as String?
    bintray {
        user = findProperty("bintray.user")
        key = findProperty("bintray.apikey")
        publish = release
        setPublications(publicationName)
        pkg.apply {
            repo = "maven"
            name = project.name
            desc = description
            websiteUrl = mavenUrl
            issueTrackerUrl = "$mavenUrl/issues"
            githubRepo = gitHub
            githubReleaseNotesFile = "README.md"
            vcsUrl = "$mavenUrl.git"
            setLabels("kotlin", "java", "pinboard", "poster", "bookmarks")
            publicDownloadNumbers = true
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

    buildScan {
        setTermsOfServiceUrl("https://gradle.com/terms-of-service")
        setTermsOfServiceAgree("yes")
    }

    "release" {
        description = "Publishes version ${project.version} to Bintray."
        group = PublishingPlugin.PUBLISH_TASK_GROUP
        dependsOn(bintrayUpload)
        doFirst {
            release = true
        }
    }
}
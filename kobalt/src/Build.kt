import com.beust.kobalt.buildScript
import com.beust.kobalt.glob
import com.beust.kobalt.plugin.application.application
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.packaging.install
import com.beust.kobalt.plugin.publish.autoGitTag
import com.beust.kobalt.plugin.publish.bintray
import com.beust.kobalt.project
import net.thauvin.erik.kobalt.plugin.versioneye.versionEye
import org.apache.maven.model.Developer
import org.apache.maven.model.License
import org.apache.maven.model.Model
import org.apache.maven.model.Scm
import java.io.File
import java.io.FileInputStream
import java.util.*

val bs = buildScript {
    plugins("net.thauvin.erik:kobalt-versioneye:", "net.thauvin.erik:kobalt-maven-local:")
}

val p = project {
    name = "pinboard-poster"
    group = "net.thauvin.erik"
    description = "Pinboard Poster for Kotlin/Java"
    artifactId = name
    version = "0.9.2"

    val localProperties = Properties().apply {
        val f = "local.properties"
        if (File(f).exists()) FileInputStream(f).use { fis -> load(fis) }
    }
    val apiToken = localProperties.getProperty("pinboard-api-token", "")

    pom = Model().apply {
        description = project.description
        url = "https://github.com/ethauvin/pinboard-poster"
        licenses = listOf(License().apply {
            name = "BSD 3-Clause"
            url = "https://opensource.org/licenses/BSD-3-Clause"
        })
        scm = Scm().apply {
            url = "https://github.com/ethauvin/pinboard-poster"
            connection = "https://github.com/ethauvin/pinboard-poster.git"
            developerConnection = "git@github.com:ethauvin/pinboard-poster.git"
        }
        developers = listOf(Developer().apply {
            id = "ethauvin"
            name = "Erik C. Thauvin"
            email = "erik@thauvin.net"
        })
    }

    dependencies {
        compile("org.jetbrains.kotlin:kotlin-stdlib:1.1.2-4")
        compile("com.squareup.okhttp3:okhttp:3.8.0")
    }

    dependenciesTest {
        //compile("org.testng:testng:6.11")
        //compile("org.jetbrains.kotlin:kotlin-test:1.1.2-3")
    }

    assemble {
        jar { }
        mavenJars { }
    }

    application {
        mainClass = "net.thauvin.erik.pinboard.PinboardPosterKt"
        args(apiToken)
    }

    application {
        taskName = "runJava"
        mainClass = "net.thauvin.erik.pinboard.JavaExample"
        args(apiToken)
    }

    install {
        target = "deploy"
        include(from("kobaltBuild/libs"), to(target), glob("**/*"))
        collect(compileDependencies).forEach {
            copy(from(it.file.absolutePath), to(target))
        }
    }

    autoGitTag {
        enabled = true
        //push = false
        message = "Version $version"
    }

    bintray {
        publish = true
        description = "Release version $version"
        issueTrackerUrl = "https://github.com/ethauvin/pinboard-poster/issues"
        vcsTag = version
        sign = true
    }

    versionEye {
        org = "Thauvin"
        team = "Owners"
        pom = true
    }
}

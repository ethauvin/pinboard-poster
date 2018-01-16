import com.beust.kobalt.buildScript
import com.beust.kobalt.glob
import com.beust.kobalt.plugin.application.application
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.packaging.install
import com.beust.kobalt.plugin.publish.autoGitTag
import com.beust.kobalt.plugin.publish.bintray
import com.beust.kobalt.project
import net.thauvin.erik.kobalt.plugin.pom2xml.pom2xml
import org.apache.maven.model.Developer
import org.apache.maven.model.License
import org.apache.maven.model.Model
import org.apache.maven.model.Scm

val bs = buildScript {
    plugins("net.thauvin.erik:kobalt-pom2xml:", "net.thauvin.erik:kobalt-maven-local:")
}

val p = project {
    name = "pinboard-poster"
    group = "net.thauvin.erik"
    description = "Pinboard Poster for Kotlin/Java"
    artifactId = name
    version = "0.9.3"

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
        compile("org.jetbrains.kotlin:kotlin-stdlib:1.2.10")
        compile("com.squareup.okhttp3:okhttp:3.9.1")
    }

    dependenciesTest {
        compile("org.testng:testng:6.12")
    }

    assemble {
        jar { }
        mavenJars { }
    }

    application {
        mainClass = "net.thauvin.erik.pinboard.PinboardPosterKt"
        ignoreErrorStream = true
    }

    application {
        taskName = "runJava"
        mainClass = "net.thauvin.erik.pinboard.JavaExample"
        ignoreErrorStream = true
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
        push = false
        message = "Version $version"
    }

    bintray {
        publish = true
        description = "Release version $version"
        issueTrackerUrl = "https://github.com/ethauvin/pinboard-poster/issues"
        vcsTag = version
        sign = true
    }

    pom2xml {

    }
}
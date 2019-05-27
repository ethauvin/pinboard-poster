import com.beust.kobalt.*
import com.beust.kobalt.plugin.application.*
import com.beust.kobalt.plugin.packaging.assemble

// ./kobaltw run

val bs = buildScript {
    repos(localMaven())
}

val p = project {
    name = "KotlinExample"
    version = "0.1"

    dependencies {
        compile("net.thauvin.erik:pinboard-poster:1.0.1")
    }

    assemble {
        jar {

        }
    }

    application {
        ignoreErrorStream = true
        mainClass = "net.thauvin.erik.pinboard.samples.KotlinExampleKt"
    }
}

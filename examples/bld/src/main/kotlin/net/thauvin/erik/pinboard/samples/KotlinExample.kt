package net.thauvin.erik.pinboard.samples

import net.thauvin.erik.pinboard.PinboardPoster
import java.nio.file.Paths
import java.util.logging.ConsoleHandler
import java.util.logging.Level

fun main(args: Array<String>) {
    val url = "https://example.com/pinboard"

    val poster = if (args.size == 1) {
        // API Token is an argument
        PinboardPoster(args[0])
    } else {
        // API Token is in local.properties or PINBOARD_API_TOKEN environment variable
        PinboardPoster(Paths.get("local.properties"))
    }

    // Set logging levels
    with(poster.logger) {
        addHandler(ConsoleHandler().apply { level = Level.FINE })
        level = Level.FINE
        useParentHandlers = false
    }

    // Add Pin
    if (poster.addPin(url, "Testing", "Extended test", tags = listOf("test", "kotlin"))) {
        println("Added: $url")
    }

    // Delete Pin
    if (poster.deletePin(url)) {
        println("Deleted: $url")
    }
}

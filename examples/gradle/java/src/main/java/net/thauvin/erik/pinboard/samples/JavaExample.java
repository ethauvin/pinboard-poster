package net.thauvin.erik.pinboard.samples;

import net.thauvin.erik.pinboard.PinConfig;
import net.thauvin.erik.pinboard.PinboardPoster;

import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaExample {
    public static void main(String[] args) {
        final String url = "https://example.com/pinboard";
        final PinboardPoster poster;

        if (args.length == 1) {
            // API Token is an argument
            poster = new PinboardPoster(args[0]);
        } else {
            // API Token is in local.properties or PINBOARD_API_TOKEN environment variable
            poster = new PinboardPoster(Paths.get("local.properties"));
        }

        // Set logging levels
        final ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINE);
        final Logger logger = poster.getLogger();
        logger.addHandler(consoleHandler);
        logger.setLevel(Level.FINE);
        logger.setUseParentHandlers(false);

        // Add Pin
        if (poster.addPin(new PinConfig.Builder(url, "Testing")
                .extended("Extra")
                .tags("test", "java")
                .build())) {
            System.out.println("Added: " + url);
        }

        // Delete Pin
        if (poster.deletePin(url)) {
            System.out.println("Deleted: " + url);
        }
    }
}

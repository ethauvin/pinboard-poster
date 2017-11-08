/*
 * JavaExample.java
 *
 * Copyright (c) 2017, Erik C. Thauvin (erik@thauvin.net)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *   Neither the name of this project nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.thauvin.erik.pinboard;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaExample {
    public static void main(String[] args) {
        final String url = "http://www.example.com/pinboard";
        final Path properties = Paths.get("local.properties");
        final PinboardPoster poster;

        if (args.length == 1) {
            // API Token is an argument
            poster = new PinboardPoster(args[0]);
        } else if (Files.exists(properties)) {
            // API Token is in local.properties (PINBOARD_API_TOKEN)
            final Properties p = new Properties();
            try (final InputStream stream = Files.newInputStream(properties)) {
                p.load(stream);
            } catch (IOException ignore) {
                ;
            }
            poster = new PinboardPoster(p);
        } else {
            // API Token is an environment variable (PINBOARD_API_TOKEN) or empty
            poster = new PinboardPoster();
        }

        // Set logging levels
        final ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINE);
        final Logger logger = poster.getLogger();
        logger.addHandler(consoleHandler);
        logger.setLevel(Level.FINE);

        // Add Pin
        if (poster.addPin(url, "Testing", "Extended test", "test kotlin")) {
            System.out.println("Added: " + url);
        }

        // Delete Pin
        if (poster.deletePin(url)) {
            System.out.println("Deleted: " + url);
        }
    }
}
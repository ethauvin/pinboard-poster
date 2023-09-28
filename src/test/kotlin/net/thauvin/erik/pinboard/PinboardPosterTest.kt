/*
 * PinboardPosterTest.kt
 *
 * Copyright (c) 2017-2023, Erik C. Thauvin (erik@thauvin.net)
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

package net.thauvin.erik.pinboard

import org.testng.Assert.*
import org.testng.annotations.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZonedDateTime
import java.util.*
import java.util.logging.Level

class PinboardPosterTest {
    private val url = "http://www.example.com/?random=" + (1000..10000).random()
    private val desc = "This is a test."
    private val localProps = Paths.get("local.properties")
    private val isCi = "true" == System.getenv("CI")

    @Test
    fun testAddPin() {
        var poster = PinboardPoster("")
        poster.logger.level = Level.FINE

        assertFalse(poster.addPin(url, desc), "apiToken: <blank>")

        poster.apiToken = "foo"
        assertFalse(poster.addPin(url, desc), "apiToken: ${poster.apiToken}")

        // poster.apiToken = "foo:TESTING"
        // assertFalse(poster.addPin(url, desc), "apiToken: ${poster.apiToken}")

        poster = PinboardPoster(localProps)
        if (!isCi) {
            poster.logger.level = Level.FINE
        }
        assertTrue(poster.addPin(url, desc), "apiToken: ${Constants.ENV_API_TOKEN}")
    }

    @Test
    fun testAddPinConfig() {
        val poster = PinboardPoster(localProps)
        if (!isCi) {
            poster.logger.level = Level.FINE
        }

        var config = PinConfig.Builder().url(url).description(desc).extended("extra")

        assertTrue(poster.addPin(config.build()), "apiToken: ${Constants.ENV_API_TOKEN}")

        config = config.tags("foo", "bar")
        assertTrue(poster.addPin(config.build()), "tags(foo,bar)")

        config = config.shared(false)
        assertTrue(poster.addPin(config.build()), "shared(false)")

        try {
            assertFalse(poster.addPin(config.replace(false).build()))
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("item already exists"))
        }

        config = config.replace(true).toRead(true)
        assertTrue(poster.addPin(config.build()), "toRead(true)")

        config = config.dt(ZonedDateTime.now())
        assertTrue(poster.addPin(config.build()), "dt(now)")
    }

    @Test
    fun testDeletePin() {
        val props = if (Files.exists(localProps)) {
            Properties().apply {
                Files.newInputStream(localProps).use { nis -> load(nis) }
            }
        } else {
            Properties().apply {
                setProperty(Constants.ENV_API_TOKEN, System.getenv(Constants.ENV_API_TOKEN))
            }
        }

        var poster = PinboardPoster(props)
        poster.logger.level = Level.FINE

        poster.apiEndPoint = ""
        assertFalse(poster.deletePin(url), "apiEndPoint: <blank>")

        poster = PinboardPoster(localProps, Constants.ENV_API_TOKEN)
        if (!isCi) {
            poster.logger.level = Level.FINE
        }
        poster.apiEndPoint = Constants.API_ENDPOINT
        assertTrue(poster.deletePin(url), "apiEndPoint: ${Constants.API_ENDPOINT}")

        expectThrows(IOException::class.java) {
            poster.parseMethodResponse("post/delete", "<result code=\"item not found\"/>")
        }

        expectThrows(IOException::class.java) {
            poster.parseMethodResponse("post/delete", "")
        }

        assertFalse(poster.deletePin("foo.com"), "url: foo.com")
    }
}

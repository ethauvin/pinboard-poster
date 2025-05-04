/*
 * PinboardPosterTest.kt
 *
 * Copyright (c) 2017-2025, Erik C. Thauvin (erik@thauvin.net)
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

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZonedDateTime
import java.util.*
import java.util.logging.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PinboardPosterTest {
    private val desc = "This is a test."
    private val localProperties = Paths.get("local.properties")
    private val loggerLevel = Level.FINE

    private fun getLocalProperties(): Properties {
        return if (Files.exists(localProperties)) {
            Properties().apply { Files.newInputStream(localProperties).use { nis -> load(nis) } }
        } else {
            Properties().apply { setProperty(Constants.ENV_API_TOKEN, System.getenv(Constants.ENV_API_TOKEN)) }
        }
    }

    private fun newPinboardPoster(): PinboardPoster = PinboardPoster().apply { logger.level = loggerLevel }

    private fun newPinboardPoster(apiToken: String): PinboardPoster =
        PinboardPoster(apiToken).apply { logger.level = loggerLevel }

    private fun newPinboardPosterWithLocalProperties(): PinboardPoster =
        PinboardPoster(getLocalProperties()).apply { logger.level = loggerLevel }

    private fun randomUrl(): String = "https://www.example.com/?random=" + (1000..10000).random()

    @Nested
    @DisplayName("Add Pin Tests")
    inner class AddPinTests {
        @Test
        @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
        fun `Add a pin with invalid API token`() {
            val poster = newPinboardPoster("foo:TESTING")
            val url = randomUrl()

            assertFalse(poster.addPin(url, desc), "apiToken: ${poster.apiToken}")
        }

        @Test
        fun `Add a pin with blank API token`() {
            val poster = newPinboardPoster("  ")
            val url = randomUrl()

            assertTrue(poster.apiToken.isBlank(), "apiToken should be blank.")
            assertFalse(poster.addPin(url, desc), "apiToken: <blank>")
        }

        @Test
        fun `Add a pin with invalid API endpoint URL`() {
            val poster = newPinboardPoster("user:token")
            val url = randomUrl()

            poster.apiEndPoint = "foo"

            assertFalse(poster.addPin(url, desc), "apiEndPoint: ${poster.apiEndPoint}")
        }

        @Test
        fun `Add a pin with wrong API endpoint host`() {
            val poster = newPinboardPoster("user:token")
            val url = randomUrl()

            poster.apiEndPoint = "https://example.com/"

            assertFalse(poster.addPin(url, desc), "apiEndPoint: ${poster.apiEndPoint}")
        }

        @Test
        fun `Add a pin with blank API endpoint`() {
            val poster = newPinboardPoster("user:token")
            val url = randomUrl()

            poster.apiEndPoint = "  "

            assertTrue(poster.apiEndPoint.isBlank(), "apiEndPoint should be blank.")
            assertFalse(poster.addPin(url, desc), "apiEndPoint: ${poster.apiEndPoint}")
        }

        @Test
        fun `Add a pin`() {
            val poster = newPinboardPosterWithLocalProperties()
            val url = randomUrl()

            assertTrue(poster.validate(), "validate()")
            assertTrue(poster.addPin(url, desc), "addPin($url, $desc)")
            assertTrue(poster.deletePin(url), "deletePin($url)")
        }

        @Test
        fun `Add a pin using config`() {
            val poster = newPinboardPosterWithLocalProperties()
            val url = randomUrl()

            assertTrue(poster.validate(), "validate()")

            val config = PinConfig.Builder(url, desc).extended("extra")

            assertTrue(poster.addPin(config.build()), "apiToken: ${Constants.ENV_API_TOKEN}")

            config.tags("foo", "bar")
            assertTrue(poster.addPin(config.build()), "tags(foo,bar)")

            config.shared(false)
            assertTrue(poster.addPin(config.build()), "shared(false)")

            try {
                assertFalse(poster.addPin(config.replace(false).build()))
            } catch (e: IOException) {
                assertTrue(e.message!!.contains("item already exists"))
            }

            config.description("Yet another test.").replace(true).toRead(true)
            assertTrue(poster.addPin(config.build()), "toRead(true)")

            config.dt(ZonedDateTime.now())
            assertTrue(poster.addPin(config.build()), "dt(now)")

            assertTrue(poster.deletePin(url), "deletePin($url)")

            config.url(randomUrl())
            assertTrue(poster.addPin(config.build()), "add($url)")
            assertTrue(poster.deletePin(config.url), "delete($url)")
        }
    }

    @Test
    fun `Delete a pin`() {
        val poster = newPinboardPosterWithLocalProperties()
        val url = randomUrl()

        assertTrue(poster.addPin(url, desc), "addPin($url, $desc)")
        assertTrue(poster.deletePin(url), "deletePin($url)")

        assertThrows<IOException> {
            poster.parseMethodResponse("post/delete", "<result code=\"item not found\"/>")
        }

        assertThrows<IOException> {
            poster.parseMethodResponse("post/delete", "")
        }

        assertFalse(poster.deletePin("foo.com"), "deletePin(foo.com)")
    }

    @Nested
    @DisplayName("Clean EndPoint Tests")
    inner class CleanEndPointTests {
        private lateinit var poster: PinboardPoster

        @BeforeEach
        fun beforeEach() {
            poster = newPinboardPoster()
        }

        @Test
        fun `Endpoint with trailing slash`() {
            poster.apiEndPoint = "https://api.example.com/"
            val result = poster.cleanEndPoint("posts/add")
            assertEquals(
                "https://api.example.com/posts/add", result,
                "Endpoint should handle trailing slash correctly"
            )
        }

        @Test
        fun `Endpoint without trailing slash`() {
            poster.apiEndPoint = "https://api.example.com"
            val result = poster.cleanEndPoint("posts/add")
            assertEquals(
                "https://api.example.com/posts/add", result,
                "Endpoint should handle missing trailing slash correctly"
            )
        }

        @Test
        fun `Empty endpoint`() {
            poster.apiEndPoint = ""
            val result = poster.cleanEndPoint("")
            assertEquals("", result, "Empty endpoint should be empty")
        }

        @Test
        fun `Blank endpoint`() {
            poster.apiEndPoint = "  "
            val result = poster.cleanEndPoint("  ")
            assertEquals("  ", result, "Empty endpoint should be empty")
        }

        @Test
        fun `Default endpoint with valid method`() {
            poster.apiEndPoint = Constants.API_ENDPOINT
            val result = poster.cleanEndPoint("posts/add")
            assertEquals(
                "https://api.pinboard.in/v1/posts/add", result,
                "Default endpoint should concatenate method correctly"
            )
        }
    }

    @Nested
    @DisplayName("Validate Tests")
    inner class ValidateTests {
        private lateinit var poster: PinboardPoster

        @BeforeEach
        fun beforeEach() {
            poster = newPinboardPoster()
        }

        @Test
        fun `API token and endpoint are valid`() {
            poster.apiToken = "user:testtoken"
            poster.apiEndPoint = "https://api.example.com/v1/"
            assertTrue(poster.validate(), "Validation should pass with valid token and endpoint")
        }

        @Test
        fun `API token is missing colon`() {
            poster.apiToken = "usertesttoken" // Missing colon
            poster.apiEndPoint = "https://api.example.com/v1/" // Endpoint is valid but should not be checked
            assertFalse(poster.validate(), "Validation should fail if API token is missing a colon")
        }

        @Test
        fun `API token is empty`() {
            poster.apiToken = "" // Empty token
            poster.apiEndPoint = "https://api.example.com/v1/"
            assertFalse(poster.validate(), "Validation should fail if API token is empty")
        }

        @Test
        fun `API token is blank`() {
            poster.apiToken = "   " // Blank token (contains no colon)
            poster.apiEndPoint = "https://api.example.com/v1/"
            assertFalse(poster.validate(), "Validation should fail if API token is blank")
        }

        @Test
        fun `API token is valid but endpoint is empty`() {
            poster.apiToken = "user:testtoken"
            poster.apiEndPoint = "" // Empty endpoint
            assertFalse(poster.validate(), "Validation should fail if endpoint is empty")
        }

        @Test
        fun `API token is valid but endpoint is blank`() {
            poster.apiToken = "user:testtoken"
            poster.apiEndPoint = "   " // Blank endpoint
            assertFalse(poster.validate(), "Validation should fail if endpoint is blank")
        }

        @Test
        fun `API token is valid but endpoint is malformed`() {
            poster.apiToken = "user:testtoken"
            // This URL will cause a URISyntaxException because of the unescaped space
            poster.apiEndPoint = "https://api.example.com/v1/ with space"
            assertFalse(
                poster.validate(),
                "Validation should fail if endpoint is malformed causing URISyntaxException"
            )
        }

        @Test
        fun `API token is valid but endpoint is malformed (with invalid char)`() {
            poster.apiToken = "user:testtoken"
            poster.apiEndPoint = "https://["
            assertFalse(
                poster.validate(),
                "Validation should fail if endpoint has invalid characters causing URISyntaxException"
            )
        }

        @Test
        fun `API token is valid and endpoint is default constant`() {
            poster.apiToken = "user:testtoken"
            poster.apiEndPoint = Constants.API_ENDPOINT // Default valid endpoint
            assertTrue(
                poster.validate(),
                "Validation should pass with valid token and default valid endpoint"
            )
        }

        @Test
        fun `API token has multiple colons`() {
            poster.apiToken = "user:test:token:extra" // Multiple colons, still contains at least one
            poster.apiEndPoint = "https://api.example.com/v1/"
            assertFalse(poster.validate(), "Validation should fail if API token has multiple colons")
        }

        @Test
        fun `API token has only colon at start`() {
            poster.apiToken = ":testtoken"
            poster.apiEndPoint = "https://api.example.com/v1/"
            assertFalse(poster.validate(), "Validation should fail if API token starts with a colon")
        }

        @Test
        fun `API token has only colon at end`() {
            poster.apiToken = "user:"
            poster.apiEndPoint = "https://api.example.com/v1/"
            assertFalse(poster.validate(), "Validation should fail if API token ends with a colon")
        }

        @Test
        fun `API token is just a colon`() {
            poster.apiToken = ":"
            poster.apiEndPoint = "https://api.example.com/v1/"
            assertFalse(poster.validate(), "Validation should fail if API token is just a colon")
        }
    }
}

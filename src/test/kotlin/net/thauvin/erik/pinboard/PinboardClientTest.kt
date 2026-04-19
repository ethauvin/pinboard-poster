/*
 * PinboardClientTest.kt
 *
 * Copyright (c) 2017-2026, Erik C. Thauvin (erik@thauvin.net)
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

import net.thauvin.erik.pinboard.TestUtils.getLocalProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import java.io.File
import java.io.IOException
import java.time.ZonedDateTime
import java.util.logging.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PinboardClientTest {
    private val desc = "This is a test."

    private fun newClient(): PinboardClient =
        PinboardClient(getLocalProperties().getProperty(Constants.ENV_API_TOKEN)).apply {
            apiEndpoint = Constants.API_ENDPOINT
        }

    private fun newClient(apiToken: String): PinboardClient =
        PinboardClient(apiToken).apply { apiEndpoint = Constants.API_ENDPOINT }

    private fun randomUrl(): String = "https://www.example.com/?random=" + (1000..10000).random()

    @Nested
    @DisplayName("Add Pin Tests")
    inner class AddPinTests {

        @Test
        @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
        fun `Add a pin with invalid API token`() {
            val client = newClient("foo:TESTING")
            val url = randomUrl()

            assertFalse(client.addPin(url, desc), "apiToken: invalid")
        }

        @Test
        fun `Add a pin with blank API token`() {
            val client = newClient("  ")
            val url = randomUrl()

            assertFalse(client.addPin(url, desc), "apiToken: <blank>")
        }

        @Test
        fun `Add a pin with invalid API endpoint URL`() {
            val client = newClient("user:token")
            val url = randomUrl()

            client.apiEndpoint = "foo"

            assertFalse(client.addPin(url, desc), "apiEndpoint: ${client.apiEndpoint}")
        }

        @Test
        fun `Add a pin with wrong API endpoint host`() {
            val client = newClient("user:token")
            val url = randomUrl()

            client.apiEndpoint = "https://example.com/"

            assertFalse(client.addPin(url, desc), "apiEndpoint: ${client.apiEndpoint}")
        }

        @Test
        fun `Add a pin with blank API endpoint`() {
            val client = newClient("user:token")
            val url = randomUrl()

            client.apiEndpoint = "  "

            assertTrue(client.apiEndpoint.isBlank(), "apiEndpoint should be blank.")
            assertFalse(client.addPin(url, desc), "apiEndpoint: <blank>")
        }

        @Test
        fun `Add a pin using minimal overload`() {
            val client = newClient()
            val url = randomUrl()

            assertTrue(client.addPin(url, desc), "addPin($url, $desc)")
            assertTrue(client.deletePin(url), "deletePin($url)")
        }

        @Test
        fun `Add a pin using config`() {
            val client = newClient()
            val url = randomUrl()

            val config = PinConfig.Builder(url, desc).extended("extra")

            assertTrue(client.addPin(config.build()), "extended(extra)")

            config.tags("foo", "bar")
            assertTrue(client.addPin(config.build()), "tags(foo,bar)")

            config.shared(false)
            assertTrue(client.addPin(config.build()), "shared(false)")

            try {
                assertFalse(client.addPin(config.replace(false).build()))
            } catch (e: IOException) {
                assertTrue(e.message!!.contains("item already exists"))
            }

            config.description("Yet another test.").replace(true).toRead(true)
            assertTrue(client.addPin(config.build()), "toRead(true)")

            config.dt(ZonedDateTime.now())
            assertTrue(client.addPin(config.build()), "dt(now)")

            assertTrue(client.deletePin(url), "deletePin($url)")

            config.url(randomUrl())
            assertTrue(client.addPin(config.build()), "add(newUrl)")
            assertTrue(client.deletePin(config.url), "delete(newUrl)")
        }

        @Test
        fun `Add a pin using full Java-style overload`() {
            val client = newClient()
            val url = randomUrl()

            val result = client.addPin(
                url,
                desc,
                "extended",
                listOf("foo", "bar"),
                ZonedDateTime.now(),
                replace = true,
                shared = true,
                toRead = false
            )

            assertTrue(result, "addPin(full overload)")
            assertTrue(client.deletePin(url), "deletePin($url)")
        }

        @Test
        fun `Add a pin using Kotlin DSL`() {
            val client = newClient()
            val url = randomUrl()

            val result = client.addPin {
                url(url)
                description(desc)
                tags("dsl", "kotlin")
                toRead(true)
            }

            assertTrue(result, "addPin(DSL)")
            assertTrue(client.deletePin(url), "deletePin($url)")
        }
    }

    @Nested
    @DisplayName("Constructor & Static Factory Tests")
    inner class ConstructorTests {
        private val desc = "This is a test."
        private val loggerLevel = Level.FINE

        private fun randomUrl(): String =
            "https://www.example.com/?random=" + (1000..10000).random()

        private fun newClientFromProps(): PinboardClient =
            PinboardClient(getLocalProperties()).apply { logger.level = loggerLevel }

        @Test
        fun `Construct with API token`() {
            val client = PinboardClient("user:token")
            assertEquals("user:token", client.apiToken)
        }

        @Test
        fun `Construct with Properties`() {
            val props = getLocalProperties()
            val client = PinboardClient(props)
            assertEquals(props.getProperty(Constants.ENV_API_TOKEN), client.apiToken)
        }

        @Test
        fun `Construct with File`() {
            val file = File("local.properties")
            if (file.exists()) {
                val client = PinboardClient(file)
                assertTrue(client.apiToken.isNotBlank())
            } else {
                assertTrue(true, "local.properties not present; skipping file constructor test")
            }
        }

        @Test
        fun `Construct with Path`() {
            val path = File("local.properties").toPath()
            if (path.toFile().exists()) {
                val client = PinboardClient(path)
                assertTrue(client.apiToken.isNotBlank())
            } else {
                assertTrue(true, "local.properties not present; skipping path constructor test")
            }
        }

        @Test
        fun `Static factory with API token`() {
            val client = PinboardClient.of("user:token")
            assertEquals("user:token", client.apiToken)
        }

        @Test
        fun `Static factory with Properties`() {
            val props = getLocalProperties()
            val client = PinboardClient.of(props)
            assertEquals(props.getProperty(Constants.ENV_API_TOKEN), client.apiToken)
        }

        @Test
        fun `Static factory with File`() {
            val file = File("local.properties")
            if (file.exists()) {
                val client = PinboardClient.of(file)
                assertTrue(client.apiToken.isNotBlank())
            } else {
                assertTrue(true, "local.properties not present; skipping file factory test")
            }
        }

        @Test
        fun `Static factory with Path`() {
            val path = File("local.properties").toPath()
            if (path.toFile().exists()) {
                val client = PinboardClient.of(path)
                assertTrue(client.apiToken.isNotBlank())
            } else {
                assertTrue(true, "local.properties not present; skipping path factory test")
            }
        }

        @Test
        fun `Constructed client can add and delete a pin`() {
            val client = newClientFromProps()
            val url = randomUrl()

            assertTrue(client.addPin(url, desc), "addPin($url, $desc)")
            assertTrue(client.deletePin(url), "deletePin($url)")
        }

        @Test
        fun `Construct with File and property name`() {
            val file = File("local.properties")
            if (file.exists()) {
                val client = PinboardClient(file, Constants.ENV_API_TOKEN)
                assertTrue(client.apiToken.isNotBlank(), "apiToken from file+propertyName should not be blank")
            } else {
                assertTrue(true, "local.properties not present; skipping file+propertyName constructor test")
            }
        }

        @Test
        fun `Construct with Path and property name`() {
            val path = File("local.properties").toPath()
            if (path.toFile().exists()) {
                val client = PinboardClient(path, Constants.ENV_API_TOKEN)
                assertTrue(client.apiToken.isNotBlank(), "apiToken from path+propertyName should not be blank")
            } else {
                assertTrue(true, "local.properties not present; skipping path+propertyName constructor test")
            }
        }
    }


    @Nested
    @DisplayName("Delete Pin Tests")
    inner class DeletePinTests {
        @Test
        fun `Delete a pin`() {
            val client = newClient()
            val url = randomUrl()

            assertTrue(client.addPin(url, desc), "addPin($url, $desc)")
            assertTrue(client.deletePin(url), "deletePin($url)")

            val poster = PinboardPoster("user:token")
            assertThrows<IOException> {
                poster.parseMethodResponse("post/delete", "<result code=\"item not found\"/>")
            }

            assertThrows<IOException> {
                poster.parseMethodResponse("post/delete", "")
            }

            assertFalse(client.deletePin("foo.com"), "deletePin(foo.com)")
        }
    }


    @Nested
    @DisplayName("Endpoint Tests")
    inner class EndpointTests {

        private lateinit var client: PinboardClient

        @BeforeEach
        fun beforeEach() {
            client = newClient("user:token")
        }

        @Test
        fun `Endpoint with trailing slash`() {
            client.apiEndpoint = "https://api.example.com/"
            val poster = PinboardPoster("user:token")
            poster.apiEndPoint = client.apiEndpoint

            val result = poster.cleanEndPoint("posts/add")
            assertEquals("https://api.example.com/posts/add", result)
        }

        @Test
        fun `Endpoint without trailing slash`() {
            client.apiEndpoint = "https://api.example.com"
            val poster = PinboardPoster("user:token")
            poster.apiEndPoint = client.apiEndpoint

            val result = poster.cleanEndPoint("posts/add")
            assertEquals("https://api.example.com/posts/add", result)
        }
    }

    @Nested
    @DisplayName("Static Factory of() Tests")
    inner class StaticFactoryTests {
        private val desc = "This is a test."
        private val loggerLevel = Level.FINE

        private fun randomUrl(): String =
            "https://www.example.com/?random=" + (1000..10000).random()

        private fun newClientFromProps(): PinboardClient =
            PinboardClient.of(getLocalProperties()).apply { logger.level = loggerLevel }

        @Test
        fun `of with API token`() {
            val client = PinboardClient.of("user:token")
            assertEquals("user:token", client.apiToken, "apiToken should match the provided value")
        }

        @Test
        fun `of with Properties`() {
            val props = getLocalProperties()
            val client = PinboardClient.of(props)
            assertEquals(
                props.getProperty(Constants.ENV_API_TOKEN),
                client.apiToken,
                "apiToken should be read from properties"
            )
        }

        @Test
        fun `of with File`() {
            val file = File("local.properties")
            if (file.exists()) {
                val client = PinboardClient.of(file)
                assertTrue(client.apiToken.isNotBlank(), "apiToken from file should not be blank")
            } else {
                assertTrue(true, "local.properties not present; skipping file factory test")
            }
        }

        @Test
        fun `of with Path`() {
            val path = File("local.properties").toPath()
            if (path.toFile().exists()) {
                val client = PinboardClient.of(path)
                assertTrue(client.apiToken.isNotBlank(), "apiToken from path should not be blank")
            } else {
                assertTrue(true, "local.properties not present; skipping path factory test")
            }
        }

        @Test
        fun `of with Properties can add and delete a pin`() {
            val client = newClientFromProps()
            val url = randomUrl()

            assertTrue(client.addPin(url, desc), "addPin($url, $desc)")
            assertTrue(client.deletePin(url), "deletePin($url)")
        }

        @Test
        fun `of with File and property name`() {
            val file = File("local.properties")
            if (file.exists()) {
                val client = PinboardClient.of(file, Constants.ENV_API_TOKEN)
                assertTrue(client.apiToken.isNotBlank(), "apiToken from of(file, propertyName) should not be blank")
            } else {
                assertTrue(true, "local.properties not present; skipping of(file, propertyName) test")
            }
        }

        @Test
        fun `of with Path and property name`() {
            val path = File("local.properties").toPath()
            if (path.toFile().exists()) {
                val client = PinboardClient.of(path, Constants.ENV_API_TOKEN)
                assertTrue(client.apiToken.isNotBlank(), "apiToken from of(path, propertyName) should not be blank")
            } else {
                assertTrue(true, "local.properties not present; skipping of(path, propertyName) test")
            }
        }
    }
}

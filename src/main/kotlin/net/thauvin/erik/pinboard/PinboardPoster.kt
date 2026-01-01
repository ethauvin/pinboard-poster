/*
 * PinboardPoster.kt
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

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/** Constants for this package. **/
object Constants {
    /** The Pinboard API endpoint URL. **/
    const val API_ENDPOINT = "https://api.pinboard.in/v1/"

    /** The API token environment variable. **/
    const val ENV_API_TOKEN = "PINBOARD_API_TOKEN"
}

/**
 * A small Kotlin/Java library for posting to [Pinboard](https://pinboard.in/).
 *
 * @constructor Creates a new instance.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 */
class PinboardPoster() {
    /**
     * Creates a new instance using an [API Token][apiToken].
     *
     * @param apiToken The API token.
     */
    constructor(apiToken: String) : this() {
        this.apiToken = apiToken
    }

    /**
     * Creates a new instance using a [Properties][properties] and [Property Key][key].
     *
     * @param properties The properties.
     * @param key The property key.
     */
    @JvmOverloads
    constructor(properties: Properties, key: String = Constants.ENV_API_TOKEN) : this() {
        apiToken = properties.getProperty(key, apiToken)
    }

    /**
     * Creates a new instance using a [Properties File Path][propertiesFilePath] and [Property Key][key].
     *
     * @param propertiesFilePath The properties file path.
     * @param key The property key.
     */
    @JvmOverloads
    constructor(propertiesFilePath: Path, key: String = Constants.ENV_API_TOKEN) : this() {
        if (Files.exists(propertiesFilePath)) {
            apiToken = Properties().apply {
                Files.newInputStream(propertiesFilePath).use { nis ->
                    load(nis)
                }
            }.getProperty(key, apiToken)
        }
    }

    /**
     * Creates a new instance using a [Properties File][propertiesFile] and [Property Key][key].
     *
     * @param propertiesFile The properties file.
     * @param key The property key.
     */
    @Suppress("unused")
    @JvmOverloads
    constructor(propertiesFile: File, key: String = Constants.ENV_API_TOKEN) : this(propertiesFile.toPath(), key)

    /** The API token. **/
    var apiToken: String = System.getenv(Constants.ENV_API_TOKEN) ?: ""

    /** The API end point. **/
    var apiEndPoint: String = Constants.API_ENDPOINT

    /** The logger instance. **/
    val logger: Logger by lazy { Logger.getLogger(PinboardPoster::class.java.simpleName) }

    private val client by lazy {
        OkHttpClient.Builder().apply {
            if (logger.isLoggable(Level.FINE)) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }.build()
    }

    /**
     * Adds a bookmark to Pinboard using a [PinConfig] builder.
     */
    fun addPin(config: PinConfig): Boolean {
        return addPin(
            url = config.url,
            description = config.description,
            extended = config.extended,
            tags = config.tags,
            dt = config.dt,
            replace = config.replace,
            shared = config.shared,
            toRead = config.toRead
        )
    }

    /**
     * Adds a bookmark to Pinboard.
     *
     * This method supports of all the [Pinboard API Parameters](https://pinboard.in/api/#posts_add).
     *
     * @param url The URL of the bookmark.
     * @param description The title of the bookmark.
     * @param extended The description of the bookmark.
     * @param tags A list of up to 100 tags.
     * @param dt The creation time of the bookmark.
     * @param replace Replace any existing bookmark with the specified URL. Default `true`.
     * @param shared Make bookmark public. Default is `true`.
     * @param toRead Mark the bookmark as unread. Default is `false`.
     *
     * @return `true` if the bookmark was successfully added.
     */
    @JvmOverloads
    fun addPin(
        url: String,
        description: String,
        extended: String = "",
        tags: List<String> = emptyList(),
        dt: ZonedDateTime = ZonedDateTime.now(),
        replace: Boolean = true,
        shared: Boolean = true,
        toRead: Boolean = false
    ): Boolean {
        if (validate()) {
            if (!validateUrl(url)) {
                logger.severe("Please specify a valid URL to pin.")
            } else if (description.isBlank()) {
                logger.severe("Please specify a valid description to pin: `$url`")
            } else {
                val params = mapOf(
                    "url" to url,
                    "description" to description,
                    "extended" to extended,
                    "tags" to tags.joinToString(","),
                    "dt" to DateTimeFormatter.ISO_INSTANT.format(dt.withNano(0)),
                    "replace" to yesNo(replace),
                    "shared" to yesNo(shared),
                    "toread" to yesNo(toRead)
                )
                return executeMethod("posts/add", params)
            }
        }

        return false
    }

    /**
     *  Deletes a bookmark on Pinboard.
     *
     *  This method supports of all the [Pinboard API Parameters](https://pinboard.in/api/#posts_delete).
     *
     *  @param url The URL of the bookmark to delete.
     *
     *  @return `true` if the bookmark was successfully deleted.
     */
    fun deletePin(url: String): Boolean {
        if (validate()) {
            if (!validateUrl(url)) {
                logger.severe("Please specify a valid URL to delete.")
            } else {
                return executeMethod("posts/delete", mapOf("url" to url))
            }
        }

        return false
    }

    @Throws(IOException::class)
    internal fun parseMethodResponse(method: String, response: String) {
        if (response.isEmpty()) {
            throw IOException("Response for $method is empty.")
        }

        val factory = DocumentBuilderFactory.newInstance().apply {
            isValidating = false
            isIgnoringElementContentWhitespace = true
            isIgnoringComments = true
            isCoalescing = false
            isNamespaceAware = false
        }

        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.isXIncludeAware = false // correct method call
        } catch (e: ParserConfigurationException) {
            logger.log(
                Level.FINE,
                "Could not set one or more secure parser features - parser may not support them",
                e
            )
        }

        try {
            val builder = factory.newDocumentBuilder()
            builder.setEntityResolver { _, _ -> InputSource(StringReader("")) }

            val document = builder.parse(InputSource(StringReader(response)))
            val code = document.getElementsByTagName("result")?.item(0)?.attributes?.getNamedItem("code")?.nodeValue

            if (!code.isNullOrBlank()) {
                throw IOException("An error has occurred while executing $method: $code")
            } else {
                throw IOException("An error has occurred while executing $method.")
            }
        } catch (e: SAXException) {
            throw IOException("Could not parse $method response.", e)
        } catch (e: IllegalArgumentException) {
            throw IOException("Invalid input source for $method response", e)
        }
    }

    internal fun cleanEndPoint(method: String): String {
        return if (apiEndPoint.isBlank()) {
            method
        } else if (apiEndPoint.last() == '/') {
            "$apiEndPoint$method"
        } else {
            "$apiEndPoint/$method"
        }
    }

    private fun executeMethod(method: String, params: Map<String, String>): Boolean {
        val apiUrl = cleanEndPoint(method).toHttpUrlOrNull()
        if (apiUrl == null) {
            logger.severe("Invalid API end point: $apiEndPoint")
            return false
        }

        val httpUrl = apiUrl.newBuilder().apply {
            params.forEach { (k, v) -> addQueryParameter(k, v) }
            addQueryParameter("auth_token", apiToken)
        }.build()

        val request = Request.Builder().url(httpUrl).build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.warning("HTTP request failed: ${response.code} ${response.message}")
                }

                val body = response.body.string()
                if (body.contains("done")) {
                    true
                } else {
                    parseMethodResponse(method, body)
                    false
                }
            }
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "Request failed: ${e.message}", e)
            false
        }
    }

    /**
     * Ensures that the API token and end point are valid.
     */
    internal fun validate(): Boolean {
        var isValid = true
        if (!apiToken.matches("[A-Za-z0-9]+:[A-Za-z0-9]+".toRegex())) {
            logger.severe("Please specify a valid API token. (eg. user:TOKEN)")
            isValid = false
        } else if (!validateUrl(apiEndPoint)) {
            logger.severe("Please specify a valid API end point. (eg. ${Constants.API_ENDPOINT})")
            isValid = false
        }
        return isValid
    }

    private fun validateUrl(url: String): Boolean {
        if (url.isBlank()) return false

        return try {
            URI.create(url)
            true
        } catch (e: IllegalArgumentException) {
            logger.log(Level.FINE, "Invalid URL: $url", e)
            false
        }
    }

    private fun yesNo(bool: Boolean): String {
        return if (bool) {
            "yes"
        } else {
            "no"
        }
    }
}

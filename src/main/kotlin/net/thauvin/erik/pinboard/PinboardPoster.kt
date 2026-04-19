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
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.parsers.DocumentBuilderFactory

/** Constants for this package. */
object Constants {
    /** The default Pinboard API endpoint URL. */
    const val API_ENDPOINT = "https://api.pinboard.in/v1/"

    /** The environment variable name containing the API token. */
    const val ENV_API_TOKEN = "PINBOARD_API_TOKEN"
}

/**
 * A lightweight Kotlin/Java client for posting bookmarks to
 * [Pinboard](https://pinboard.in/).
 *
 * Instances may be created directly with an API token, or by loading values
 * from a `Properties` object or properties file. All network operations are
 * performed using OkHttp.
 */
class PinboardPoster() {

    /**
     * Creates a new instance with the given API token.
     *
     * @param apiToken The Pinboard API token in the form `user:TOKEN`.
     */
    constructor(apiToken: String) : this() {
        this.apiToken = apiToken
    }

    /**
     * Creates a new instance using values from a [Properties] object.
     *
     * @param properties The properties containing the API token.
     * @param key The property name holding the API token. Defaults to
     * [Constants.ENV_API_TOKEN].
     */
    @JvmOverloads
    constructor(properties: Properties, key: String = Constants.ENV_API_TOKEN) : this() {
        apiToken = properties.getProperty(key, apiToken)
    }

    /**
     * Creates a new instance using a properties file located at the given [Path].
     *
     * @param propertiesFilePath The path to the properties file.
     * @param key The property name holding the API token. Defaults to
     * [Constants.ENV_API_TOKEN].
     */
    @JvmOverloads
    constructor(propertiesFilePath: Path, key: String = Constants.ENV_API_TOKEN) : this() {
        if (Files.exists(propertiesFilePath)) {
            apiToken = Properties().apply {
                Files.newInputStream(propertiesFilePath).use { load(it) }
            }.getProperty(key, apiToken)
        }
    }

    /**
     * Creates a new instance using a properties file.
     *
     * @param propertiesFile The properties file.
     * @param key The property name holding the API token. Defaults to
     * [Constants.ENV_API_TOKEN].
     */
    @JvmOverloads
    constructor(propertiesFile: File, key: String = Constants.ENV_API_TOKEN) :
            this(propertiesFile.toPath(), key)

    /** The API token used for authentication. */
    var apiToken: String = System.getenv(Constants.ENV_API_TOKEN) ?: ""

    /** The API endpoint URL. */
    var apiEndPoint: String = Constants.API_ENDPOINT

    /** Logger instance for this class. */
    val logger: Logger by lazy { Logger.getLogger(PinboardPoster::class.java.simpleName) }

    /** Shared OkHttp client instance. */
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder().apply {
            if (logger.isLoggable(Level.FINE)) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }.build()
    }

    /**
     * Adds a bookmark using a [PinConfig] instance.
     *
     * @return `true` if the bookmark was successfully added.
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
     * This method supports all parameters described in the
     * [Pinboard API documentation](https://pinboard.in/api/#posts_add).
     *
     * @param url The URL of the bookmark.
     * @param description The title of the bookmark.
     * @param extended Optional extended description.
     * @param tags A list of tags (up to 100).
     * @param dt The creation time of the bookmark.
     * @param replace Whether to replace an existing bookmark with the same URL.
     * @param shared Whether the bookmark should be public.
     * @param toRead Whether the bookmark should be marked as unread.
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
     * Deletes a bookmark from Pinboard.
     *
     * @param url The URL of the bookmark to delete.
     * @return `true` if the bookmark was successfully deleted.
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

    /**
     * Parses a Pinboard API XML response.
     *
     * The response must contain a `<result>` element with a `code` attribute.
     * A `code` value of `"done"` indicates success. Any other value results in
     * an [IOException].
     *
     * @param method The API method being processed.
     * @param xml The raw XML response.
     *
     * @return `true` if the response indicates success.
     * @throws IOException If the XML is malformed, missing required elements,
     * or indicates an error.
     */
    @Throws(IOException::class)
    internal fun parseMethodResponse(method: String, xml: String): Boolean {
        if (xml.isBlank()) {
            throw IOException("Empty XML response for method: $method")
        }

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isIgnoringComments = true
            isIgnoringElementContentWhitespace = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }

        val document = try {
            factory.newDocumentBuilder().parse(xml.byteInputStream())
        } catch (e: SAXException) {
            throw IOException("Malformed XML for method: $method", e)
        }

        val root = document.documentElement
            ?: throw IOException("Missing root element in XML for method: $method")

        if (root.nodeName != "result") {
            throw IOException("Unexpected root element <${root.nodeName}> for method: $method")
        }

        val code = root.getAttribute("code").trim()
        if (code.isEmpty()) {
            throw IOException("Missing 'code' attribute in <result> for method: $method")
        }

        return when (code) {
            "done" -> true
            "item not found" -> throw IOException("Item not found for method: $method")
            else -> throw IOException("Unexpected result code '$code' for method: $method")
        }
    }

    /**
     * Normalizes the API endpoint and appends the given method name.
     *
     * @param method The API method path.
     * @return The fully qualified endpoint URL.
     */
    internal fun cleanEndPoint(method: String): String {
        return if (apiEndPoint.isBlank()) {
            method
        } else {
            apiEndPoint.trimEnd('/') + "/" + method
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
     * Validates the API token and endpoint.
     *
     * @return `true` if both the token and endpoint are valid.
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

    /**
     * Checks whether the given string is a valid HTTP or HTTPS URL.
     */
    private fun validateUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val parsed = url.toHttpUrlOrNull() ?: return false
        return parsed.scheme == "http" || parsed.scheme == "https"
    }

    /**
     * Converts a boolean value to `"yes"` or `"no"` for API parameters.
     */
    private fun yesNo(bool: Boolean): String = if (bool) "yes" else "no"
}

/*
 * PinboardPoster.kt
 *
 * Copyright (c) 2017-2022, Erik C. Thauvin (erik@thauvin.net)
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

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.xml.sax.InputSource
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.parsers.DocumentBuilderFactory

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
 * @author [Erik C. Thauvin](https://erik.thauvin.net/)
 */
open class PinboardPoster() {
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
    @Suppress("unused")
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
    @Suppress("MemberVisibilityCanBePrivate")
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
     * @return `true` if bookmark was successfully added.
     */
    @JvmOverloads
    fun addPin(
        url: String,
        description: String,
        extended: String = "",
        tags: String = "",
        dt: String = "",
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
                    "tags" to tags,
                    "dt" to dt,
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
     *  @return `true` if bookmark was successfully deleted.
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
        val factory = DocumentBuilderFactory.newInstance().apply {
            isValidating = false
            isIgnoringElementContentWhitespace = true
            isIgnoringComments = true
            isCoalescing = false
            isNamespaceAware = false
        }

        if (response.isEmpty()) {
            throw IOException("Response for $method is empty.")
        }

        try {
            val document = factory.newDocumentBuilder().parse(InputSource(StringReader(response)))

            val code = document.getElementsByTagName("result")?.item(0)?.attributes?.getNamedItem("code")?.nodeValue

            if (!code.isNullOrBlank()) {
                throw IOException("An error has occurred while executing $method: $code")
            } else {
                throw IOException("An error has occurred while executing $method.")
            }
        } catch (e: org.xml.sax.SAXException) {
            throw IOException("Could not parse $method response.", e)
        } catch (e: IllegalArgumentException) {
            throw IOException("Invalid input source for $method response", e)
        }
    }

    private fun cleanEndPoint(method: String): String {
        return if (apiEndPoint.last() == '/') {
            "$apiEndPoint$method"
        } else {
            "$apiEndPoint/$method"
        }
    }

    private fun executeMethod(method: String, params: Map<String, String>): Boolean {
        try {
            val apiUrl = cleanEndPoint(method).toHttpUrlOrNull()
            if (apiUrl != null) {
                val httpUrl = apiUrl.newBuilder().apply {
                    params.forEach {
                        addQueryParameter(it.key, it.value)
                    }
                    addQueryParameter("auth_token", apiToken)
                }.build()

                val request = Request.Builder().url(httpUrl).build()
                val result = client.newCall(request).execute()

                result.body?.string()?.let { response ->
                    if (response.contains("done")) {
                        return true
                    } else {
                        parseMethodResponse(method, response)
                    }
                }
            } else {
                logger.severe("Invalid API end point: $apiEndPoint")
            }
        } catch (e: IOException) {
            logger.log(Level.SEVERE, e.message, e)
        }

        return false
    }

    private fun validate(): Boolean {
        var isValid = true
        if (!apiToken.contains(':')) {
            logger.severe("Please specify a valid API token. (eg. user:TOKEN)")
            isValid = false
        } else if (!validateUrl(apiEndPoint)) {
            logger.severe("Please specify a valid API end point. (eg. ${Constants.API_ENDPOINT})")
            isValid = false
        }
        return isValid
    }

    private fun validateUrl(url: String): Boolean {
        var isValid = url.isNotBlank()
        if (isValid) {
            try {
                URL(url)
            } catch (e: MalformedURLException) {
                logger.log(Level.FINE, "Invalid URL: $url", e)
                isValid = false
            }
        }
        return isValid
    }

    private fun yesNo(bool: Boolean): String {
        return if (bool) {
            "yes"
        } else {
            "no"
        }
    }
}

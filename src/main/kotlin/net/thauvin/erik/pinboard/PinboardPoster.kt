/*
 * PinboardPoster.kt
 *
 * Copyright (c) 2017-2018, Erik C. Thauvin (erik@thauvin.net)
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

import net.thauvin.erik.pinboard.Constants.ENV_API_TOKEN
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.parsers.DocumentBuilderFactory

/** Constants **/
object Constants {
    /** The Pinboard API endpoint URL. **/
    const val API_ENDPOINT = "https://api.pinboard.in/v1/"
    /** The API token environment variable. **/
    const val ENV_API_TOKEN = "PINBOARD_API_TOKEN"
}

/**
 * A small Kotlin/Java library for posting to [Pinboard](https://pinboard.in/).
 *
 * @author [Erik C. Thauvin](https://erik.thauvin.net/)
 */
open class PinboardPoster() {
    /**
     * Initialize from an API token.
     *
     * @param apiToken The API token.
     */
    constructor(apiToken: String) : this() {
        this.apiToken = apiToken
    }

    /**
     * Initialize from a properties and key.
     *
     * @param properties The properties.
     * @param key The property key.
     */
    @Suppress("unused")
    @JvmOverloads
    constructor(properties: Properties, key: String = ENV_API_TOKEN) : this() {
        apiToken = properties.getProperty(key, apiToken)
    }

    /**
     * Initialize fom a properties file path and key.
     *
     * @param propertiesFilePath The properties file path.
     * @param key The property key.
     */
    @JvmOverloads
    constructor(propertiesFilePath: Path, key: String = ENV_API_TOKEN) : this() {
        if (Files.exists(propertiesFilePath)) {
            apiToken = Properties().apply {
                Files.newInputStream(propertiesFilePath).use { nis ->
                    load(nis)
                }
            }.getProperty(key, apiToken)
        }
    }

    /**
     * Initialize fom a properties file and key.
     *
     * @param propertiesFile The properties file.
     * @param key The property key.
     */
    @Suppress("unused")
    @JvmOverloads
    constructor(propertiesFile: File, key: String = ENV_API_TOKEN) : this(propertiesFile.toPath(), key)

    /** The API token. **/
    var apiToken: String = if (System.getenv(ENV_API_TOKEN).isNullOrBlank()) "" else System.getenv(ENV_API_TOKEN)

    /** The API end point. **/
    var apiEndPoint: String = Constants.API_ENDPOINT

    /** The logger object. **/
    val logger: Logger by lazy { Logger.getLogger(PinboardPoster::class.java.simpleName) }

    private val client by lazy { OkHttpClient() }

    /**
     * Add a bookmark to Pinboard.
     *
     * This method supports of all the [Pinboard API Parameters](https://pinboard.in/api/#posts_add).
     *
     * @param url The URL of the bookmark.
     * @param description The title of the bookmark.
     * @param extended The description of the bookmark.
     * @param tags A list of up to 100 tags.
     * @param dt   The creation time of the bookmark.
     * @param replace  Replace any existing bookmark with the specified URL. Default `true`.
     * @param shared   Make bookmark public. Default is `true`.
     * @param toRead Mark the bookmark as unread. Default is `false`.
     *
     * @return `true` if bookmark was successfully added.
     */
    @JvmOverloads
    fun addPin(url: String,
               description: String,
               extended: String = "",
               tags: String = "",
               dt: String = "",
               replace: Boolean = true,
               shared: Boolean = true,
               toRead: Boolean = false): Boolean {
        if (validate()) {
            if (!validateUrl(url)) {
                logger.severe("Please specify a valid URL to pin.")
            } else if (description.isBlank()) {
                logger.severe("Please specify a valid description.")
            } else {
                val params = listOf(
                        Pair("url", url),
                        Pair("description", description),
                        Pair("extended", extended),
                        Pair("tags", tags),
                        Pair("dt", dt),
                        Pair("replace", yesNo(replace)),
                        Pair("shared", yesNo(shared)),
                        Pair("toread", yesNo(toRead))
                )
                return executeMethod("posts/add", params)
            }
        }

        return false
    }

    /**
     *  Delete a bookmark on Pinboard.
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
                return executeMethod("posts/delete", listOf(Pair("url", url)))
            }
        }

        return false
    }

    private fun executeMethod(method: String, params: List<Pair<String, String>>): Boolean {
        val apiUrl = HttpUrl.parse(cleanEndPoint(method))
        if (apiUrl != null) {
            val httpUrl = apiUrl.newBuilder().apply {
                params.forEach {
                    if (it.second.isNotBlank()) {
                        addQueryParameter(it.first, it.second)
                    }
                }
                addQueryParameter("auth_token", apiToken)
            }.build()

            val request = Request.Builder().url(httpUrl).build()
            val result = client.newCall(request).execute()

            logHttp(method, "HTTP Result: ${result.code()}")

            val response = result.body()?.string()

            if (response != null) {
                logHttp(method, "HTTP Response:\n$response")
                if (response.contains("done")) {
                    return true
                } else {
                    val factory = DocumentBuilderFactory.newInstance().apply {
                        isValidating = false
                        isIgnoringElementContentWhitespace = true
                        isIgnoringComments = true
                        isCoalescing = false
                        isNamespaceAware = false
                    }

                    try {
                        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(response)))

                        val code = document.getElementsByTagName("result")?.item(0)?.attributes?.getNamedItem("code")?.nodeValue

                        if (code != null && code.isNotBlank()) {
                            logger.severe("An error has occurred while executing $method: $code")
                        } else {
                            logger.severe("An error has occurred while executing $method.")
                        }
                    } catch (e: Exception) {
                        logger.log(Level.SEVERE, "Could not parse $method XML response.", e)
                    }
                }
            }
        } else {
            logger.severe("Invalid API end point: $apiEndPoint")
        }

        return false
    }

    private fun cleanEndPoint(method: String): String {
        return if (apiEndPoint.endsWith('/')) {
            "$apiEndPoint$method"
        } else {
            "$apiEndPoint/$method"
        }
    }

    private fun logHttp(method: String, msg: String) {
        logger.logp(Level.FINE, PinboardPoster::class.java.name, "executeMethod($method)", msg)
    }

    private fun validate(): Boolean {
        if (apiToken.isBlank() || !apiToken.contains(':')) {
            logger.severe("Please specify a valid API token. (eg. user:TOKEN)")
            return false
        } else if (!validateUrl(apiEndPoint)) {
            logger.severe("Please specify a valid API end point. (eg. ${Constants.API_ENDPOINT})")
            return false
        }
        return true
    }

    private fun validateUrl(url: String): Boolean {
        if (url.isBlank()) {
            return false
        }

        try {
            URL(url)
        } catch (e: Exception) {
            logger.log(Level.FINE, "Invalid URL: $url", e)
            return false
        }

        return true
    }

    private fun yesNo(bool: Boolean): String {
        return if (bool) {
            "yes"
        } else {
            "no"
        }
    }
}
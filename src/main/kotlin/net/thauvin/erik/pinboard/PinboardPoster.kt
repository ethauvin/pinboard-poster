/*
 * PinboardPoster.kt
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
package net.thauvin.erik.pinboard

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

object Constants {
    const val API_ENDPOINT = "https://api.pinboard.in/v1/"
    const val AUTH_TOKEN = "auth_token"
    const val DONE = "done"
}

open class PinboardPoster(val apiToken: String) {
    var apiEndPoint: String = Constants.API_ENDPOINT

    val logger: Logger by lazy { Logger.getLogger(PinboardPoster::class.java.simpleName) }

    private val client by lazy { OkHttpClient() }

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
                logger.log(Level.SEVERE, "Please specify a valid URL to pin.")
            } else if (description.isBlank()) {
                logger.log(Level.SEVERE, "Please specify a valid description.")
            } else {
                val apiUrl = HttpUrl.parse(cleanEndPoint("posts/add"))
                if (apiUrl != null) {
                    val httpUrl = apiUrl.newBuilder().apply {
                        addQueryParameter("url", url)
                        addQueryParameter("description", description)
                        if (extended.isNotBlank()) {
                            addQueryParameter("extended", extended)
                        }
                        if (tags.isNotBlank()) {
                            addQueryParameter("tags", tags)
                        }
                        if (dt.isNotBlank()) {
                            addQueryParameter("dt", dt)
                        }
                        if (!replace) {
                            addQueryParameter("replace", "no")
                        }
                        if (!shared) {
                            addQueryParameter("shared", "no")
                        }
                        if (toRead) {
                            addQueryParameter("toread", "yes")
                        }
                        addQueryParameter(Constants.AUTH_TOKEN, apiToken)
                    }.build()

                    val request = Request.Builder().url(httpUrl).build()
                    val result = client.newCall(request).execute()

                    logger.log(Level.FINE, "HTTP Result: ${result.code()}")

                    val response = result.body()?.string()

                    if (response != null && response.contains(Constants.DONE)) {
                        logger.log(Level.FINE, "HTTP Response:\n$response")
                        return true
                    }
                } else {
                    logger.log(Level.SEVERE, "Invalid API end point: $apiEndPoint")
                }
            }
        }

        return false
    }

    fun deletePin(url: String): Boolean {
        if (validate()) {
            if (!validateUrl(url)) {
                logger.log(Level.SEVERE, "Please specify a valid URL to delete.")
            } else {
                val apiUrl = HttpUrl.parse(cleanEndPoint("posts/delete"))
                if (apiUrl != null) {
                    val httpUrl = apiUrl.newBuilder().apply {
                        addQueryParameter("url", url)
                        addQueryParameter(Constants.AUTH_TOKEN, apiToken)
                    }.build()

                    val request = Request.Builder().url(httpUrl).build()
                    val result = client.newCall(request).execute()

                    logger.log(Level.FINE, "HTTP Result: ${result.code()}")

                    val response = result.body()?.string()

                    if (response != null && response.contains(Constants.DONE)) {
                        logger.log(Level.FINE, "HTTP Response:\n$response")
                        return true
                    }
                } else {
                    logger.log(Level.SEVERE, "Invalid API end point: $apiEndPoint")
                }
            }
        }

        return false
    }

    private fun validateUrl(url: String): Boolean {
        if (url.isBlank()) {
            return false
        }

        try {
            URL(url)
        } catch(e: Exception) {
            logger.log(Level.FINE, "Invalid URL: $url", e)
            return false
        }

        return true
    }

    private fun validate(): Boolean {
        if (apiToken.isBlank() && !apiToken.contains(':')) {
            logger.log(Level.SEVERE, "Please specify a valid API token. (eg. user:TOKEN)")
            return false
        } else if (!validateUrl(apiEndPoint)) {
            logger.log(Level.SEVERE, "Please specify a valid API end point. (eg. ${Constants.API_ENDPOINT})")
            return false
        }
        return true
    }

    private fun cleanEndPoint(method: String): String {
        if (apiEndPoint.endsWith('/')) {
            return "$apiEndPoint$method"
        } else {
            return "$apiEndPoint/$method"
        }
    }
}

fun main(args: Array<String>) {
    if (args.size == 1) {
        val url = "http://www.example.com/pinboard"
        val poster = PinboardPoster(args[0])

        with(poster.logger) {
            addHandler(ConsoleHandler().apply { level = Level.FINE })
            level = Level.FINE
        }

        if (poster.addPin(url, "Testing", "Extended test", "test koltin")) {
            println("Added: $url")
        }

        if (poster.deletePin(url)) {
            println("Deleted: $url")
        }
    } else {
        println("Please specify a valid API token. (eg. user:TOKEN)")
    }
}
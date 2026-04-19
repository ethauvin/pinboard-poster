/*
 * PinboardClient.kt
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

import java.io.File
import java.nio.file.Path
import java.time.ZonedDateTime
import java.util.Properties
import java.util.logging.Logger

/**
 * A Java‑friendly facade over [PinboardPoster] and [PinConfig].
 *
 * This class provides a stable API surface for Java callers while delegating
 * all functionality to the underlying Kotlin implementation. Kotlin callers
 * may use the DSL or the underlying classes directly.
 */
class PinboardClient private constructor(
    private val poster: PinboardPoster
) {

    /**
     * Creates a new client using the given API token.
     *
     * @param apiToken The Pinboard API token in the form `user:TOKEN`.
     */
    constructor(apiToken: String) : this(PinboardPoster(apiToken))

    /**
     * Creates a new client using values from a [Properties] object.
     *
     * @param properties The properties containing the API token.
     */
    constructor(properties: Properties) : this(PinboardPoster(properties))

    /**
     * Creates a new client using a properties file.
     *
     * @param file The properties file.
     */
    constructor(file: File) : this(PinboardPoster(file))

    /**
     * Creates a new client using a properties file and a specific property name.
     *
     * @param file The properties file.
     * @param propertyName The property name holding the API token.
     */
    constructor(file: File, propertyName: String) : this(PinboardPoster(file, propertyName))

    /**
     * Creates a new client using a properties file path.
     *
     * @param path The path to the properties file.
     */
    constructor(path: Path) : this(PinboardPoster(path))

    /**
     * Creates a new client using a properties file path and a specific property name.
     *
     * @param path The path to the properties file.
     * @param propertyName The property name holding the API token.
     */
    constructor(path: Path, propertyName: String) : this(PinboardPoster(path, propertyName))

    companion object {
        /**
         * Creates a new client using the given API token.
         */
        @JvmStatic
        fun of(apiToken: String): PinboardClient = PinboardClient(apiToken)

        /**
         * Creates a new client using values from a [Properties] object.
         */
        @JvmStatic
        fun of(properties: Properties): PinboardClient = PinboardClient(properties)

        /**
         * Creates a new client using a properties file.
         */
        @JvmStatic
        fun of(file: File): PinboardClient = PinboardClient(file)

        /**
         * Creates a new client using a properties file and a specific property name.
         */
        @JvmStatic
        fun of(file: File, propertyName: String): PinboardClient =
            PinboardClient(file, propertyName)

        /**
         * Creates a new client using a properties file path.
         */
        @JvmStatic
        fun of(path: Path): PinboardClient = PinboardClient(path)

        /**
         * Creates a new client using a properties file path and a specific property name.
         */
        @JvmStatic
        fun of(path: Path, propertyName: String): PinboardClient =
            PinboardClient(path, propertyName)
    }

    /**
     * Gets or sets the API token used by the underlying [PinboardPoster].
     */
    var apiToken: String
        get() = poster.apiToken
        set(value) {
            poster.apiToken = value
        }

    /**
     * Returns the logger used by the underlying [PinboardPoster].
     */
    val logger: Logger
        get() = poster.logger

    /**
     * Adds a bookmark using a [PinConfig] instance.
     *
     * @return `true` if the bookmark was successfully added.
     */
    fun addPin(config: PinConfig): Boolean =
        poster.addPin(config)

    /**
     * Adds a bookmark with only the required fields.
     *
     * @return `true` if the bookmark was successfully added.
     */
    fun addPin(url: String, description: String): Boolean =
        poster.addPin(url, description)

    /**
     * Adds a bookmark with all optional parameters.
     *
     * This overload exists for Java callers who prefer not to use
     * [PinConfig.Builder].
     *
     * @return `true` if the bookmark was successfully added.
     */
    fun addPin(
        url: String,
        description: String,
        extended: String?,
        tags: List<String>?,
        dt: ZonedDateTime?,
        replace: Boolean,
        shared: Boolean,
        toRead: Boolean
    ): Boolean {
        val config = PinConfig.Builder(url, description)
            .extended(extended ?: "")
            .tags(tags ?: emptyList())
            .dt(dt ?: ZonedDateTime.now())
            .replace(replace)
            .shared(shared)
            .toRead(toRead)
            .build()

        return poster.addPin(config)
    }

    /**
     * Deletes a bookmark by URL.
     *
     * @return `true` if the bookmark was successfully deleted.
     */
    fun deletePin(url: String): Boolean =
        poster.deletePin(url)

    /**
     * Gets or sets the API endpoint used by the underlying [PinboardPoster].
     */
    var apiEndpoint: String
        get() = poster.apiEndPoint
        set(value) {
            poster.apiEndPoint = value
        }
}

/**
 * Adds a bookmark using a Kotlin DSL.
 *
 * Example:
 * ```
 * client.addPin {
 *     url("https://example.com")
 *     description("Example")
 *     tags("kotlin", "api")
 *     toRead(true)
 * }
 * ```
 *
 * @return `true` if the bookmark was successfully added.
 */
@Suppress("unused", "RedundantSuppression")
fun PinboardClient.addPin(block: PinConfig.Builder.() -> Unit): Boolean {
    val builder = PinConfig.Builder("", "")
    builder.block()
    return addPin(builder.build())
}

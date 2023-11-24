/*
 * PinConfig.kt
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

import java.time.ZonedDateTime

/**
 * Provides a builder to add a pin.
 *
 * Supports of all the [Pinboard API Parameters](https://pinboard.in/api/#posts_add).
 */
class PinConfig private constructor(
    val url: String,
    val description: String,
    val extended: String,
    val tags: Array<out String>,
    val dt: ZonedDateTime,
    val replace: Boolean,
    val shared: Boolean,
    val toRead: Boolean
) {
    /**
     * Configures the parameters to add a pin.
     */
    data class Builder(
        private var url: String = "",
        private var description: String = "",
        private var extended: String = "",
        private var tags: Array<out String> = emptyArray(),
        private var dt: ZonedDateTime = ZonedDateTime.now(),
        private var replace: Boolean = true,
        private var shared: Boolean = true,
        private var toRead: Boolean = false
    ) {
        fun url(url: String) = apply { this.url = url }
        fun description(description: String) = apply { this.description = description }
        fun extended(extended: String) = apply { this.extended = extended }
        fun tags(vararg tag: String) = apply { this.tags = tag }
        fun dt(datetime: ZonedDateTime) = apply { this.dt = datetime }
        fun replace(replace: Boolean) = apply { this.replace = replace }
        fun shared(shared: Boolean) = apply { this.shared = shared }
        fun toRead(toRead: Boolean) = apply { this.toRead = toRead }

        fun build() = PinConfig(
            url,
            description,
            extended,
            tags,
            dt,
            replace,
            shared,
            toRead
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Builder

            if (url != other.url) return false
            if (description != other.description) return false
            if (extended != other.extended) return false
            if (!tags.contentEquals(other.tags)) return false
            if (dt != other.dt) return false
            if (replace != other.replace) return false
            if (shared != other.shared) return false
            if (toRead != other.toRead) return false

            return true
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + description.hashCode()
            result = 31 * result + extended.hashCode()
            result = 31 * result + tags.contentHashCode()
            result = 31 * result + dt.hashCode()
            result = 31 * result + replace.hashCode()
            result = 31 * result + shared.hashCode()
            result = 31 * result + toRead.hashCode()
            return result
        }

        override fun toString(): String {
            return "Builder(url='$url', description='$description', extended='$extended'," +
                    "tags=${tags.contentToString()}, dt=$dt, replace=$replace, shared=$shared, toRead=$toRead)"
        }


    }
}

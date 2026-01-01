/*
 * PinConfigTest.kt
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

import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PinConfigTest {
    @Test
    fun `Builder with mandatory fields`() {
        val pinConfig = PinConfig.Builder("https://example.com", "Example Description").build()

        assertEquals("https://example.com", pinConfig.url)
        assertEquals("Example Description", pinConfig.description)
        assertEquals("", pinConfig.extended)
        assertTrue(pinConfig.tags.isEmpty())
        assertTrue(pinConfig.replace)
        assertTrue(pinConfig.shared)
        assertEquals(false, pinConfig.toRead)
    }

    @Test
    fun `Builder with extended description`() {
        val extendedText = "Extended description for testing"
        val pinConfig = PinConfig.Builder("https://example.com", "Example Description")
            .extended(extendedText)
            .build()

        assertEquals(extendedText, pinConfig.extended)
    }

    @Test
    fun `Builder with tags`() {
        val tags = arrayOf("tag1", "tag2", "tag3")
        val pinConfig = PinConfig.Builder("https://example.com", "Example Description")
            .tags(*tags)
            .build()

        assertEquals(tags.toList(), pinConfig.tags)
    }

    @Test
    fun `Builder with custom datetime`() {
        val customDateTime = ZonedDateTime.parse("1997-08-29T02:14:00-04:00")
        val pinConfig = PinConfig.Builder("https://example.com", "Example Description")
            .dt(customDateTime)
            .build()

        assertEquals(customDateTime, pinConfig.dt)
    }

    @Test
    fun `Builder with replace set to false`() {
        val pinConfig = PinConfig.Builder("https://example.com", "Example Description")
            .replace(false)
            .build()

        assertEquals(false, pinConfig.replace)
    }

    @Test
    fun `Builder with shared set to false`() {
        val pinConfig = PinConfig.Builder("https://example.com", "Example Description")
            .shared(false)
            .build()

        assertEquals(false, pinConfig.shared)
    }

    @Test
    fun `Builder with toRead set to true`() {
        val pinConfig = PinConfig.Builder("https://example.com", "Example Description")
            .toRead(true)
            .build()

        assertEquals(true, pinConfig.toRead)
    }

    @Test
    fun `Builder updating url`() {
        val pinConfig = PinConfig.Builder("https://example.com", "Example Description")
            .url("https://new-url.com")
            .build()

        assertEquals("https://new-url.com", pinConfig.url)
    }

    @Test
    fun `Builder updating description`() {
        val pinConfig = PinConfig.Builder("https://example.com", "Example Description")
            .description("Updated Description")
            .build()

        assertEquals("Updated Description", pinConfig.description)
    }
}

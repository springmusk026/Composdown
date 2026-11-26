package io.github.springmusk026.composdown.compose.internal

import androidx.compose.ui.unit.LayoutDirection
import io.github.springmusk026.composdown.core.MarkdownParser
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.ListKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RenderingLogicTest {
    private fun block(markdown: String): Block = MarkdownParser().parse(markdown).blocks.single()

    @Test
    fun directionFollowsTheFirstLetter() {
        assertEquals(LayoutDirection.Rtl, firstStrongDirection("123 שלום world"))
        assertEquals(LayoutDirection.Rtl, firstStrongDirection("مرحبا"))
        assertEquals(LayoutDirection.Ltr, firstStrongDirection("Hello שלום"))
        assertNull(firstStrongDirection("123 - !"))
    }

    @Test
    fun blockDirectionLooksInsideContainers() {
        assertEquals(LayoutDirection.Rtl, block("- שלום\n- עולם\n").contentDirection())
        assertEquals(LayoutDirection.Rtl, block("> مرحبا\n").contentDirection())
        assertNull(block("```\ncode\n```\n").contentDirection())
    }

    @Test
    fun onlyWebEmailAndPhoneLinksAreSafeByDefault() {
        assertTrue(isSafeLink("https://example.com"))
        assertTrue(isSafeLink("HTTP://example.com"))
        assertTrue(isSafeLink("mailto:a@b.co"))
        assertTrue(isSafeLink("tel:+123"))
        assertFalse(isSafeLink("file:///etc/passwd"))
        assertFalse(isSafeLink("javascript:alert(1)"))
        assertFalse(isSafeLink("/relative/path"))
    }

    @Test
    fun orderedMarkersCountFromTheStartNumberWithTheSourceDelimiter() {
        val ordered = (block("3) a\n") as Block.ListBlock).kind as ListKind.Ordered
        assertEquals("3)", orderedMarker(ordered, index = 0))
        assertEquals("4)", orderedMarker(ordered, index = 1))
    }

    @Test
    fun standaloneImageIsDetected() {
        assertTrue((block("![a](b.png)") as Block.Paragraph).standaloneImage() != null)
        assertNull((block("text ![a](b.png)") as Block.Paragraph).standaloneImage())
    }
}

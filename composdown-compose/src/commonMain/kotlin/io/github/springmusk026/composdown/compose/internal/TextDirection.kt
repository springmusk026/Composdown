package io.github.springmusk026.composdown.compose.internal

import androidx.compose.ui.unit.LayoutDirection
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.plainText

/** Code points of scripts written right to left (Hebrew, Arabic, Syriac, Thaana, N'Ko, and others). */
private val RTL_RANGES =
    listOf(
        0x0590..0x08FF,
        0xFB1D..0xFDFF,
        0xFE70..0xFEFF,
        0x10800..0x10FFF,
        0x1E800..0x1EFFF,
    )

private const val MIN_SUPPLEMENTARY = 0x10000
private const val SURROGATE_SHIFT = 10

/**
 * The layout direction of [block]'s first strongly directional character, or `null` if it has none
 * (for example, a block of digits or punctuation, which then follows the surrounding direction).
 * Lists, quotes and tables are laid out in this direction, so bullets and bars sit on the correct side.
 */
internal fun Block.contentDirection(): LayoutDirection? = firstText()?.let(::firstStrongDirection)

private fun Block.firstText(): String? =
    when (this) {
        is Block.Paragraph -> {
            content.plainText()
        }

        is Block.Heading -> {
            content.plainText()
        }

        is Block.BlockQuote -> {
            children.firstNotNullOfOrNull { it.firstText() }
        }

        is Block.Alert -> {
            children.firstNotNullOfOrNull { it.firstText() }
        }

        is Block.ListBlock -> {
            items.firstOrNull()?.children?.firstNotNullOfOrNull { it.firstText() }
        }

        is Block.Table -> {
            header.cells
                .firstOrNull()
                ?.content
                ?.plainText()
        }

        is Block.CodeBlock, is Block.HtmlBlock, Block.ThematicBreak -> {
            null
        }
    }

/** Direction of the first letter in [text]: right to left for RTL scripts, left to right for others. */
internal fun firstStrongDirection(text: String): LayoutDirection? {
    var i = 0
    while (i < text.length) {
        val c = text[i]
        val codePoint =
            if (c.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
                ((c.code - Char.MIN_HIGH_SURROGATE.code) shl SURROGATE_SHIFT) +
                    (text[i + 1].code - Char.MIN_LOW_SURROGATE.code) + MIN_SUPPLEMENTARY
            } else {
                c.code
            }
        if (RTL_RANGES.any { codePoint in it }) return LayoutDirection.Rtl
        if (c.isLetter()) return LayoutDirection.Ltr
        i += if (codePoint >= MIN_SUPPLEMENTARY) 2 else 1
    }
    return null
}

/** Whether a paragraph consists of a single image, which is then drawn as a block. */
internal fun Block.Paragraph.standaloneImage(): Inline.Image? = content.singleOrNull() as? Inline.Image

package io.github.springmusk026.composdown.compose.internal

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import io.github.springmusk026.composdown.compose.ComposdownTheme
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.plainText

/** Footnote reference numbers are drawn smaller, as superscripts. */
private const val FOOTNOTE_REFERENCE_SIZE = 0.75f

/**
 * Builds the styled text for inline content. Links become [LinkAnnotation]s, which are clickable and
 * exposed to accessibility services; [openLink] is called with a link's destination when activated.
 */
internal fun buildInlineText(
    inlines: List<Inline>,
    theme: ComposdownTheme,
    openLink: (String) -> Unit,
): AnnotatedString =
    buildAnnotatedString {
        val builder = InlineTextBuilder(this, theme, openLink)
        builder.append(inlines)
    }

private class InlineTextBuilder(
    private val out: AnnotatedString.Builder,
    private val theme: ComposdownTheme,
    private val openLink: (String) -> Unit,
) {
    private val linkStyles =
        TextLinkStyles(style = theme.typography.link.merge(SpanStyle(color = theme.colors.link)))

    fun append(inlines: List<Inline>) {
        for (inline in inlines) append(inline)
    }

    private fun append(inline: Inline) {
        when (inline) {
            is Inline.Text -> {
                out.append(inline.literal)
            }

            is Inline.Code -> {
                out.withStyle(inlineCodeStyle()) { append(inline.literal) }
            }

            is Inline.Emphasis -> {
                out.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(inline.children) }
            }

            is Inline.Strong -> {
                out.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(inline.children) }
            }

            is Inline.Strikethrough -> {
                out.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(inline.children)
                }
            }

            is Inline.Link -> {
                appendLink(inline)
            }

            // Images inside text are shown as their description; standalone images are drawn as blocks.
            is Inline.Image -> {
                out.withStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                ) { append(inline.children.plainText()) }
            }

            is Inline.FootnoteReference -> {
                out.withStyle(footnoteReferenceStyle()) { append(inline.number.toString()) }
            }

            // TeX source in the math style; a block component can render formulas.
            is Inline.Math -> {
                out.withStyle(theme.typography.math) { append(inline.literal) }
            }

            // Raw HTML is never interpreted; its source is shown as text.
            is Inline.HtmlInline -> {
                out.append(inline.literal)
            }

            Inline.SoftBreak -> {
                out.append(' ')
            }

            Inline.HardBreak -> {
                out.append('\n')
            }
        }
    }

    private fun appendLink(link: Inline.Link) {
        if (link.destination.isEmpty()) {
            // A link whose destination is still being streamed: styled, but not clickable yet.
            out.withStyle(linkStyles.style ?: SpanStyle()) { append(link.children) }
            return
        }
        val listener = LinkInteractionListener { openLink(link.destination) }
        out.withLink(LinkAnnotation.Url(link.destination, linkStyles, listener)) { append(link.children) }
    }

    private fun inlineCodeStyle(): SpanStyle =
        theme.typography.inlineCode.merge(
            SpanStyle(color = theme.colors.code, background = theme.colors.inlineCodeBackground),
        )

    private fun footnoteReferenceStyle(): SpanStyle =
        SpanStyle(
            color = theme.colors.link,
            fontSize = FOOTNOTE_REFERENCE_SIZE.em,
            baselineShift = BaselineShift.Superscript,
        )
}

/** URL schemes the default link handler opens. Other links need an explicit `onLinkClick`. */
private val SAFE_LINK_SCHEMES = setOf("http", "https", "mailto", "tel")

/**
 * Whether the default handler may open [destination]. Markdown from untrusted sources (such as a
 * language model) can contain links with any scheme, so only web, email and phone links are opened.
 */
internal fun isSafeLink(destination: String): Boolean {
    val scheme = destination.substringBefore(':', missingDelimiterValue = "").lowercase()
    return scheme in SAFE_LINK_SCHEMES
}

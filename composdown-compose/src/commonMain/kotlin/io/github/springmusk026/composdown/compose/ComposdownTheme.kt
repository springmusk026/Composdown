package io.github.springmusk026.composdown.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import io.github.springmusk026.composdown.compose.internal.Poko
import io.github.springmusk026.composdown.core.MarkdownExtension
import io.github.springmusk026.composdown.core.ast.AlertType

/**
 * The style of rendered Markdown: [colors], [typography], [spacing], and the [strings] Composdown adds.
 *
 * Create one with [ComposdownDefaults.theme], which derives every value from the current
 * [MaterialTheme], and override what you need.
 */
@Immutable
@Poko
public class ComposdownTheme internal constructor(
    /** Colors of rendered elements. */
    public val colors: ComposdownColors,
    /** Text styles of rendered elements. */
    public val typography: ComposdownTypography,
    /** Spacing between and inside rendered elements. */
    public val spacing: ComposdownSpacing,
    /** Text that Composdown adds to documents, such as alert titles, for localization. */
    public val strings: ComposdownStrings,
)

/** Colors of rendered Markdown elements. Create with [ComposdownDefaults.colors]. */
@Immutable
@Poko
public class ComposdownColors internal constructor(
    /** Body text. */
    public val text: Color,
    /** Link text. */
    public val link: Color,
    /** Text of code spans and code blocks. */
    public val code: Color,
    /** Background of code blocks. */
    public val codeBlockBackground: Color,
    /** Background of inline code spans. */
    public val inlineCodeBackground: Color,
    /** The bar beside block quotes. */
    public val blockQuoteBar: Color,
    /** Text inside block quotes. */
    public val blockQuoteText: Color,
    /** Thematic breaks, table borders and the line above footnotes. */
    public val divider: Color,
    /** Background of table header rows. */
    public val tableHeaderBackground: Color,
    /** List bullets and numbers. */
    public val listMarker: Color,
    /** Accent color of each kind of alert (bar and title). */
    public val alerts: Map<AlertType, Color>,
)

/** Text styles of rendered Markdown elements. Create with [ComposdownDefaults.typography]. */
@Immutable
@Poko
public class ComposdownTypography internal constructor(
    /** Paragraphs and other body text. */
    public val paragraph: TextStyle,
    /** Headings of levels 1 to 6, in order. */
    public val headings: List<TextStyle>,
    /** Code blocks. */
    public val codeBlock: TextStyle,
    /** Code spans inside text. */
    public val inlineCode: SpanStyle,
    /** Link text. */
    public val link: SpanStyle,
    /** Table header cells. */
    public val tableHeader: TextStyle,
    /** Table body cells. */
    public val tableCell: TextStyle,
    /** Footnote definitions at the end of the document. */
    public val footnote: TextStyle,
    /** The title line of alerts. */
    public val alertTitle: TextStyle,
    /** Inline math (TeX source). */
    public val math: SpanStyle,
    /** Math blocks (TeX source). */
    public val mathBlock: TextStyle,
) {
    init {
        require(headings.size == HEADING_LEVELS) { "Expected $HEADING_LEVELS heading styles, got ${headings.size}" }
    }

    /** The style of a heading of [level] (1-6). */
    public fun heading(level: Int): TextStyle = headings[level.coerceIn(1, HEADING_LEVELS) - 1]

    private companion object {
        const val HEADING_LEVELS = 6
    }
}

/** Spacing of rendered Markdown elements. Create with [ComposdownDefaults.spacing]. */
@Immutable
@Poko
public class ComposdownSpacing internal constructor(
    /** Vertical space between consecutive blocks. */
    public val block: Dp,
    /** Vertical space between items of a tight list. */
    public val tightListItem: Dp,
    /** Width reserved for list markers. */
    public val listMarkerWidth: Dp,
    /** Padding inside code blocks. */
    public val codeBlockPadding: Dp,
    /** Space between a block quote's bar and its content. */
    public val blockQuoteIndent: Dp,
    /** Width of a block quote's bar. */
    public val blockQuoteBarWidth: Dp,
    /** Padding inside table cells. */
    public val tableCellPadding: Dp,
)

/**
 * Text that Composdown adds to rendered documents. The defaults are English; supply translations to
 * localize them. Create with [ComposdownDefaults.strings].
 */
@Immutable
@Poko
public class ComposdownStrings internal constructor(
    /** The title shown at the top of each kind of alert. */
    public val alertTitles: Map<AlertType, String>,
    /** The accessibility label of a footnote's link back to its reference, followed by its number. */
    public val backToReference: String,
)

/** Default values for Composdown, derived from [MaterialTheme]. */
public object ComposdownDefaults {
    /**
     * The syntax recognized by default: GitHub Flavored Markdown plus GitHub alerts, which language
     * models commonly produce.
     */
    public val Extensions: Set<MarkdownExtension> = MarkdownExtension.GFM + MarkdownExtension.ALERTS

    /** No replacements: every block uses its default rendering. */
    public val Components: ComposdownComponents = ComposdownComponents()

    /** A theme from the current [MaterialTheme], with any of its parts replaced. */
    @Composable
    @ReadOnlyComposable
    public fun theme(
        colors: ComposdownColors = colors(),
        typography: ComposdownTypography = typography(),
        spacing: ComposdownSpacing = spacing(),
        strings: ComposdownStrings = strings(),
    ): ComposdownTheme = ComposdownTheme(colors, typography, spacing, strings)

    /** English strings; any of them can be replaced. */
    public fun strings(
        alertTitles: Map<AlertType, String> =
            mapOf(
                AlertType.NOTE to "Note",
                AlertType.TIP to "Tip",
                AlertType.IMPORTANT to "Important",
                AlertType.WARNING to "Warning",
                AlertType.CAUTION to "Caution",
            ),
        backToReference: String = "Back to reference",
    ): ComposdownStrings = ComposdownStrings(alertTitles, backToReference)

    /** Colors from the current [MaterialTheme.colorScheme]; any of them can be replaced. */
    @Composable
    @ReadOnlyComposable
    public fun colors(
        text: Color = MaterialTheme.colorScheme.onSurface,
        link: Color = MaterialTheme.colorScheme.primary,
        code: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        codeBlockBackground: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
        inlineCodeBackground: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        blockQuoteBar: Color = MaterialTheme.colorScheme.outlineVariant,
        blockQuoteText: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        divider: Color = MaterialTheme.colorScheme.outlineVariant,
        tableHeaderBackground: Color = MaterialTheme.colorScheme.surfaceContainer,
        listMarker: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        alerts: Map<AlertType, Color> =
            mapOf(
                AlertType.NOTE to MaterialTheme.colorScheme.primary,
                AlertType.TIP to MaterialTheme.colorScheme.tertiary,
                AlertType.IMPORTANT to MaterialTheme.colorScheme.secondary,
                AlertType.WARNING to WARNING_COLOR,
                AlertType.CAUTION to MaterialTheme.colorScheme.error,
            ),
    ): ComposdownColors =
        ComposdownColors(
            text,
            link,
            code,
            codeBlockBackground,
            inlineCodeBackground,
            blockQuoteBar,
            blockQuoteText,
            divider,
            tableHeaderBackground,
            listMarker,
            alerts,
        )

    /** Text styles from the current [MaterialTheme.typography]; any of them can be replaced. */
    @Composable
    @ReadOnlyComposable
    public fun typography(
        paragraph: TextStyle = MaterialTheme.typography.bodyLarge.withContentDirection(),
        headings: List<TextStyle> =
            with(MaterialTheme.typography) {
                listOf(headlineMedium, headlineSmall, titleLarge, titleMedium, titleSmall, labelLarge)
                    .map { it.withContentDirection().copy(fontWeight = FontWeight.SemiBold) }
            },
        codeBlock: TextStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        inlineCode: SpanStyle = SpanStyle(fontFamily = FontFamily.Monospace, fontSize = INLINE_CODE_SIZE.em),
        link: SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline),
        tableHeader: TextStyle =
            MaterialTheme.typography.bodyMedium.withContentDirection().copy(
                fontWeight = FontWeight.SemiBold,
            ),
        tableCell: TextStyle = MaterialTheme.typography.bodyMedium.withContentDirection(),
        footnote: TextStyle = MaterialTheme.typography.bodySmall.withContentDirection(),
        alertTitle: TextStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        // Math is shown as TeX source, where every character (backslashes included) must be legible.
        math: SpanStyle = SpanStyle(fontFamily = FontFamily.Monospace),
        mathBlock: TextStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
    ): ComposdownTypography =
        ComposdownTypography(
            paragraph,
            headings,
            codeBlock,
            inlineCode,
            link,
            tableHeader,
            tableCell,
            footnote,
            alertTitle,
            math,
            mathBlock,
        )

    /** Default spacing; any value can be replaced. */
    public fun spacing(
        block: Dp = 12.dp,
        tightListItem: Dp = 4.dp,
        listMarkerWidth: Dp = 24.dp,
        codeBlockPadding: Dp = 12.dp,
        blockQuoteIndent: Dp = 12.dp,
        blockQuoteBarWidth: Dp = 4.dp,
        tableCellPadding: Dp = 8.dp,
    ): ComposdownSpacing =
        ComposdownSpacing(
            block,
            tightListItem,
            listMarkerWidth,
            codeBlockPadding,
            blockQuoteIndent,
            blockQuoteBarWidth,
            tableCellPadding,
        )

    /** Material 3 has no warning color; this amber works on light and dark surfaces. */
    private val WARNING_COLOR = Color(0xFFB26A00)

    /** Code spans are slightly smaller than the surrounding text, as monospace fonts run large. */
    private const val INLINE_CODE_SIZE = 0.9f

    /** Text direction follows the content, so right-to-left paragraphs render correctly. */
    private fun TextStyle.withContentDirection(): TextStyle = copy(textDirection = TextDirection.Content)
}

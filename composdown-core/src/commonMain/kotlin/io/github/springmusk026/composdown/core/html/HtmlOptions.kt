package io.github.springmusk026.composdown.core.html

import io.github.springmusk026.composdown.core.internal.Poko

/**
 * Options of [toHtml].
 *
 * The defaults are safe for Markdown from untrusted sources: raw HTML is shown as text and links
 * with dangerous schemes are removed. The output still needs no further escaping to be embedded in
 * a page.
 *
 * @property rawHtml what to do with raw HTML in the Markdown.
 * @property safeLinks whether to remove the destination of links and images whose scheme can run
 *   code or read local files (`javascript:`, `vbscript:`, `file:`, and `data:` other than images),
 *   as the CommonMark reference implementation's safe mode does.
 * @property headingIds whether to give headings an `id` attribute with their
 *   [anchor][io.github.springmusk026.composdown.core.ast.DocumentHeading.anchor], so that `#anchor`
 *   links work as on GitHub.
 * @property softBreak the HTML written for a soft line break: a line feed by default (displayed as a
 *   space by browsers), or for example `<br />` to keep the source's line breaks.
 */
@Poko
public class HtmlOptions(
    public val rawHtml: RawHtml = RawHtml.ESCAPE,
    public val safeLinks: Boolean = true,
    public val headingIds: Boolean = false,
    public val softBreak: String = "\n",
) {
    /** Predefined options. */
    public companion object {
        /** Safe defaults for any input. */
        public val Default: HtmlOptions = HtmlOptions()

        /**
         * The output of the CommonMark reference implementations (cmark and commonmark.js, and
         * cmark-gfm for extensions), with raw HTML passed through and links unchanged. Use it only
         * for trusted Markdown.
         */
        public val CommonMark: HtmlOptions = HtmlOptions(rawHtml = RawHtml.ALLOW, safeLinks = false)
    }
}

/** What [toHtml] does with raw HTML blocks and inline HTML in the Markdown. */
public enum class RawHtml {
    /** Writes it as escaped text, so it is displayed instead of interpreted. */
    ESCAPE,

    /** Leaves it out. */
    OMIT,

    /**
     * Writes it unchanged, as the CommonMark specification describes. Only for trusted Markdown: raw
     * HTML can contain scripts.
     */
    ALLOW,
}

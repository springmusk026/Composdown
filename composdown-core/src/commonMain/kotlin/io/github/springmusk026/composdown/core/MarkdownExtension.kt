package io.github.springmusk026.composdown.core

/**
 * Optional syntax that a [MarkdownParser] can recognize in addition to CommonMark.
 *
 * Every extension is independent and can be enabled on its own. [GFM] is the set that makes up
 * GitHub Flavored Markdown.
 */
public enum class MarkdownExtension {
    /** Pipe tables with optional column alignment (GFM). */
    TABLES,

    /** `- [ ]` and `- [x]` task list items (GFM). */
    TASK_LISTS,

    /** `~text~` and `~~text~~` strikethrough (GFM). */
    STRIKETHROUGH,

    /**
     * Links recognized without angle brackets: `www.` addresses, `http(s)://` and `ftp://` URLs, and
     * email addresses (GFM).
     */
    AUTOLINKS,

    /** Footnote references (`[^1]`) and definitions (`[^1]: text`), as supported on GitHub. */
    FOOTNOTES,

    /**
     * GitHub alerts: a top-level block quote whose first line is `[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`,
     * `[!WARNING]` or `[!CAUTION]`.
     */
    ALERTS,
    ;

    /** Predefined sets of extensions. */
    public companion object {
        /** GitHub Flavored Markdown: tables, task lists, strikethrough, autolinks and footnotes. */
        public val GFM: Set<MarkdownExtension> = setOf(TABLES, TASK_LISTS, STRIKETHROUGH, AUTOLINKS, FOOTNOTES)
    }
}

package io.github.springmusk026.composdown.core.internal.inline

import io.github.springmusk026.composdown.core.internal.text.charAtOrEnd
import io.github.springmusk026.composdown.core.internal.text.isAsciiAlphanumeric
import io.github.springmusk026.composdown.core.internal.text.isAsciiLetter

/**
 * Scanners for GitHub Flavored Markdown extended autolinks (`www.example.com`,
 * `https://example.com`, `user@example.com`), following the GFM specification and the behavior of
 * cmark-gfm, the reference implementation.
 */
internal object ExtendedAutolinks {
    /** Characters after which a `www.` autolink may start (besides whitespace and the start of the text). */
    private const val WWW_PRECEDING_DELIMITERS = "*_~("

    /** Trailing characters that are never part of an autolink. */
    private const val TRAILING_PUNCTUATION = "?!.,:*_~'\""

    /** Characters allowed in the local part of an email autolink, besides ASCII letters and digits. */
    private const val EMAIL_LOCAL_PUNCTUATION = ".+-_"

    /** Whether a `www.` autolink may start at [index], given the character before it. */
    fun mayStartWwwAt(
        text: CharSequence,
        index: Int,
    ): Boolean {
        if (index == 0) return true
        val previous = text[index - 1]
        return previous.isAsciiWhitespace() || previous in WWW_PRECEDING_DELIMITERS
    }

    /** Matches a `www.` autolink at [start]; returns the end index (exclusive) or -1. */
    fun matchWww(
        text: CharSequence,
        start: Int,
    ): Int {
        if (!text.startsWith("www.", start)) return -1
        if (scanDomain(text, start, allowNoDots = false) < 0) return -1
        return extendAndTrim(text, start, start)
    }

    /**
     * Matches the part of a URL autolink from the `:` at [colon], after the scheme that precedes it.
     * Returns the end index (exclusive) or -1.
     */
    fun matchUrlAfterScheme(
        text: CharSequence,
        colon: Int,
    ): Int {
        if (!text.startsWith("://", colon)) return -1
        val domainStart = colon + "://".length
        if (scanDomain(text, domainStart, allowNoDots = true) < 0) return -1
        return extendAndTrim(text, colon, domainStart)
    }

    /**
     * Whether [scheme] (the letters before `:`) starts a URL autolink: `http`, `https` or `ftp`. The
     * GFM specification's prose lists only the first two, but its examples (and cmark-gfm) include `ftp`.
     */
    fun isAutolinkScheme(scheme: String): Boolean = AUTOLINK_SCHEMES.any { it.equals(scheme, ignoreCase = true) }

    private val AUTOLINK_SCHEMES = listOf("http", "https", "ftp")

    /** Characters an email autolink may extend back over: its local part and a `mailto:` or `xmpp:` protocol. */
    fun isEmailRewindChar(c: Char): Boolean = c.isAsciiAlphanumeric() || c in EMAIL_LOCAL_PUNCTUATION || c == ':'

    /** The result of rewinding from an `@` over the local part of an email address. */
    class EmailStart(
        /** Number of characters before the `@` that belong to the link. */
        val length: Int,
        /** Whether the link starts with an explicit `mailto:` or `xmpp:` protocol. */
        val hasProtocol: Boolean,
        val isXmpp: Boolean,
    )

    /**
     * Finds how far an email autolink extends before its `@`, given the plain text [before] it: over
     * letters, digits and `.+-_`, and over a `mailto:` or `xmpp:` protocol that is not preceded by a
     * letter or digit. Returns `null` if the local part would be empty.
     */
    fun rewindEmail(before: String): EmailStart? {
        var i = before.length
        var protocol: String? = null
        var atStart = false
        while (i > 0 && !atStart) {
            val c = before[i - 1]
            val protocolHere = if (c == ':') protocolEndingAt(before, i) else null
            when {
                c.isAsciiAlphanumeric() || c in EMAIL_LOCAL_PUNCTUATION -> {
                    i--
                }

                protocolHere != null -> {
                    // The link starts at the protocol, which must not be preceded by a letter or digit.
                    protocol = protocolHere
                    i -= protocolHere.length
                    atStart = true
                }

                else -> {
                    atStart = true
                }
            }
        }
        val length = before.length - i
        return if (length == 0) null else EmailStart(length, protocol != null, protocol == "xmpp:")
    }

    /** `mailto:` or `xmpp:` ending at [end] in [text] and not preceded by a letter or digit. */
    private fun protocolEndingAt(
        text: String,
        end: Int,
    ): String? =
        listOf("mailto:", "xmpp:").firstOrNull { protocol ->
            val start = end - protocol.length
            start >= 0 && text.startsWith(protocol, start) && (start == 0 || !text[start - 1].isAsciiAlphanumeric())
        }

    /**
     * Matches the domain of an email autolink from the `@` at [at]. Returns the end index (exclusive)
     * or -1. The domain needs at least one period, and `/` is allowed only for `xmpp:` addresses.
     */
    fun matchEmailDomain(
        text: CharSequence,
        at: Int,
        isXmpp: Boolean,
    ): Int {
        var i = at + 1
        var periods = 0
        while (i < text.length && isEmailDomainChar(text, i, isXmpp)) {
            if (text[i] == '.') periods++
            i++
        }
        val last = text[i - 1]
        val endsWell = last.isAsciiLetter() || last == '.'
        if (i - at < 2 || periods == 0 || !endsWell) return -1
        val end = trimTrailing(text, at, i)
        return if (end > at + 1) end else -1
    }

    /** Letters, digits, `-`, `_`, a period followed by a letter or digit, and `/` for xmpp. */
    private fun isEmailDomainChar(
        text: CharSequence,
        index: Int,
        isXmpp: Boolean,
    ): Boolean {
        val c = text[index]
        return when (c) {
            '-', '_' -> true
            '.' -> text.charAtOrEnd(index + 1).isAsciiAlphanumeric()
            '/' -> isXmpp
            else -> c.isAsciiAlphanumeric()
        }
    }

    /**
     * Scans a domain at [start]: segments of letters, digits, hyphens and underscores (and non-ASCII
     * characters) separated by periods, where the last two segments contain no underscores. Returns
     * the end index of the domain or -1.
     */
    private fun scanDomain(
        text: CharSequence,
        start: Int,
        allowNoDots: Boolean,
    ): Int {
        var i = start
        var periods = 0
        var underscoresInLast = 0
        var underscoresInPrevious = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '_' -> {
                    underscoresInLast++
                }

                c == '.' -> {
                    underscoresInPrevious = underscoresInLast
                    underscoresInLast = 0
                    periods++
                }

                !c.isHostChar() -> {
                    break
                }
            }
            i++
        }
        // Underscores are allowed in subdomains but not in the last two segments of the domain.
        val underscoresAllowed = underscoresInLast == 0 && underscoresInPrevious == 0
        val valid = i > start && underscoresAllowed && (allowNoDots || periods > 0)
        return if (valid) i else -1
    }

    /** Letters, digits, `-`, and any non-ASCII character other than whitespace. */
    private fun Char.isHostChar(): Boolean =
        isAsciiAlphanumeric() || this == '-' || (code >= ASCII_LIMIT && !isWhitespace())

    /** Extends a link from [start] to the next whitespace or `<`, then trims trailing punctuation. */
    private fun extendAndTrim(
        text: CharSequence,
        linkStart: Int,
        contentStart: Int,
    ): Int {
        var end = contentStart
        while (end < text.length && !text[end].isAsciiWhitespace() && text[end] != '<') end++
        val trimmed = trimTrailing(text, linkStart, end)
        return if (trimmed > contentStart) trimmed else -1
    }

    /**
     * Removes characters that end a sentence rather than a URL: trailing punctuation, an entity-like
     * `&name;` suffix, and closing parentheses without a matching opening one in the link.
     */
    private fun trimTrailing(
        text: CharSequence,
        start: Int,
        end: Int,
    ): Int {
        // Parentheses are counted once and the counts updated as characters are trimmed, so that a
        // long run of `)` is trimmed in linear time.
        var opening = 0
        var closing = 0
        for (i in start until end) {
            when (text[i]) {
                '(' -> opening++
                ')' -> closing++
            }
        }
        var linkEnd = end
        while (linkEnd > start) {
            val last = text[linkEnd - 1]
            val newEnd =
                when {
                    last in TRAILING_PUNCTUATION -> linkEnd - 1
                    last == ';' -> trimEntitySuffix(text, start, linkEnd)
                    last == ')' && closing > opening -> linkEnd - 1
                    else -> return linkEnd
                }
            for (i in newEnd until linkEnd) {
                when (text[i]) {
                    '(' -> opening--
                    ')' -> closing--
                }
            }
            linkEnd = newEnd
        }
        return linkEnd
    }

    /** For a link ending in `;`, removes an `&letters;` suffix if present, otherwise just the `;`. */
    private fun trimEntitySuffix(
        text: CharSequence,
        start: Int,
        end: Int,
    ): Int {
        var i = end - 2
        while (i > start && text[i].isAsciiLetter()) i--
        return if (i < end - 2 && text[i] == '&') i else end - 1
    }

    private fun Char.isAsciiWhitespace(): Boolean =
        this == ' ' || this == '\t' || this == '\n' || this == '\u000B' || this == '\u000C' || this == '\r'

    private const val ASCII_LIMIT = 0x80
}

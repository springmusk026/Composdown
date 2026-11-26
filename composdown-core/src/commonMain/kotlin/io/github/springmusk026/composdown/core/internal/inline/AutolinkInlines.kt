package io.github.springmusk026.composdown.core.internal.inline

import io.github.springmusk026.composdown.core.internal.text.isAsciiLetter

/**
 * How many characters of already-parsed text an extended autolink may take back (a URL scheme or the
 * local part of an email address). Bounding this keeps parsing linear on hostile input.
 */
private const val MAX_AUTOLINK_REWIND = 256

/**
 * Parses a GFM `http://`, `https://` or `ftp://` autolink at the `:` at [pos], after the scheme
 * letters that were already added to [block] as text. On success, moves the scheme into a link
 * appended to [block] and returns the end of the link; otherwise returns -1.
 */
internal fun parseUrlAutolink(
    block: InlineNode,
    subject: String,
    pos: Int,
): Int {
    val before = block.rewindableText { it.isAsciiLetter() }
    var schemeLength = 0
    while (schemeLength < before.length && before[before.length - 1 - schemeLength].isAsciiLetter()) schemeLength++
    val scheme = before.takeLast(schemeLength)
    if (!ExtendedAutolinks.isAutolinkScheme(scheme)) return -1
    val end = ExtendedAutolinks.matchUrlAfterScheme(subject, pos)
    if (end < 0) return -1
    block.removeTrailingText(schemeLength)
    val url = scheme + subject.substring(pos, end)
    block.appendLink(url, url)
    return end
}

/** Parses a GFM `www.` autolink at [pos], linked with an `http://` destination. Returns its end or -1. */
internal fun parseWwwAutolink(
    block: InlineNode,
    subject: String,
    pos: Int,
): Int {
    if (!ExtendedAutolinks.mayStartWwwAt(subject, pos)) return -1
    val end = ExtendedAutolinks.matchWww(subject, pos)
    if (end < 0) return -1
    val address = subject.substring(pos, end)
    block.appendLink("http://$address", address)
    return end
}

/**
 * Parses a GFM email autolink, optionally with a `mailto:` or `xmpp:` protocol, at the `@` at [pos].
 * The local part was already added to [block] as text and is moved into the link. Returns the end
 * of the link or -1.
 */
internal fun parseEmailAutolink(
    block: InlineNode,
    subject: String,
    pos: Int,
): Int {
    val before = block.rewindableText(ExtendedAutolinks::isEmailRewindChar)
    val start = ExtendedAutolinks.rewindEmail(before) ?: return -1
    val end = ExtendedAutolinks.matchEmailDomain(subject, pos, start.isXmpp)
    if (end < 0) return -1
    block.removeTrailingText(start.length)
    val address = before.takeLast(start.length) + subject.substring(pos, end)
    block.appendLink(if (start.hasProtocol) address else "mailto:$address", address)
    return end
}

/** Appends a link with a single text child. */
internal fun InlineNode.appendLink(
    destination: String,
    text: String,
) {
    val link = InlineNode(InlineNode.Kind.LINK)
    link.destination = destination
    link.appendChild(InlineNode(InlineNode.Kind.TEXT, text))
    appendChild(link)
}

/**
 * The plain text at the end of this node that an autolink may take back: up to
 * [MAX_AUTOLINK_REWIND] characters, stopping at anything other than text (such as an emphasis
 * delimiter that may still be matched) and at text containing a character the link cannot extend over.
 */
private fun InlineNode.rewindableText(canRewindOver: (Char) -> Boolean): String {
    val parts = ArrayList<String>()
    var length = 0
    var node = lastChild
    while (node != null && node.isRewindable() && length < MAX_AUTOLINK_REWIND) {
        parts.add(node.literal)
        length += node.literal.length
        // Text before a character the link cannot include is never needed.
        if (!node.literal.all(canRewindOver)) break
        node = node.previous
    }
    return parts.asReversed().joinToString("").takeLast(MAX_AUTOLINK_REWIND)
}

private fun InlineNode.isRewindable(): Boolean = kind == InlineNode.Kind.TEXT && !isActiveDelimiter

/** Removes the last [count] characters of text from the end of this node. */
private fun InlineNode.removeTrailingText(count: Int) {
    var remaining = count
    while (remaining > 0) {
        val node = checkNotNull(lastChild)
        if (node.literal.length <= remaining) {
            remaining -= node.literal.length
            node.unlink()
        } else {
            node.literal = node.literal.dropLast(remaining)
            remaining = 0
        }
    }
}

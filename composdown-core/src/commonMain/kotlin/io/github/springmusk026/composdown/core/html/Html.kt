package io.github.springmusk026.composdown.core.html

import io.github.springmusk026.composdown.core.ast.MarkdownDocument

/**
 * Renders this document as an HTML fragment.
 *
 * The markup follows the CommonMark reference implementations, and GitHub's for extensions: task
 * list items are disabled checkboxes, footnotes form a `<section class="footnotes">` at the end,
 * alerts are `<div class="markdown-alert markdown-alert-note">` elements, and math is written as TeX
 * source in elements with the class `math inline` or `math display`, ready for a client-side
 * renderer. With [HtmlOptions.CommonMark], the output is exactly that of the reference
 * implementations, verified against every example of the CommonMark and GFM specifications.
 *
 * ```
 * val html = MarkdownParser().parse(markdown).toHtml()
 * val withAnchors = document.toHtml(HtmlOptions(headingIds = true))
 * ```
 */
public fun MarkdownDocument.toHtml(options: HtmlOptions = HtmlOptions.Default): String =
    HtmlWriter(this, options).render()

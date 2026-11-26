package io.github.springmusk026.composdown.benchmarks

import io.github.springmusk026.composdown.core.MarkdownParser
import io.github.springmusk026.composdown.core.ast.DocumentHeading
import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import io.github.springmusk026.composdown.core.ast.headings
import io.github.springmusk026.composdown.core.ast.plainText
import io.github.springmusk026.composdown.core.html.HtmlOptions
import io.github.springmusk026.composdown.core.html.toHtml
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State

/** Work on a parsed document of about 73 KB: HTML rendering, plain text and headings. */
@State(Scope.Benchmark)
class DocumentBenchmark {
    private lateinit var document: MarkdownDocument

    /** Parses the input once per benchmark run. */
    @Setup
    fun setUp() {
        document = MarkdownParser().parse(BenchmarkDocuments.document(DOCUMENT_SECTIONS))
    }

    /** HTML with the default, safe options. */
    @Benchmark
    fun renderHtml(): String = document.toHtml()

    /** HTML with heading ids, which needs the document's headings. */
    @Benchmark
    fun renderHtmlWithHeadingIds(): String = document.toHtml(HtmlOptions(headingIds = true))

    /** The text of the whole document. */
    @Benchmark
    fun extractPlainText(): String = document.plainText()

    /** All headings with their anchors. */
    @Benchmark
    fun listHeadings(): List<DocumentHeading> = document.headings()

    private companion object {
        const val DOCUMENT_SECTIONS = 100
    }
}

package io.github.springmusk026.composdown.benchmarks

import io.github.springmusk026.composdown.core.IncrementalMarkdownParser
import io.github.springmusk026.composdown.core.MarkdownParser
import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State

/**
 * Parser throughput: a large document parsed in one pass, and a streamed response of 2,000 tokens
 * parsed incrementally, compared with re-parsing the whole text after every token.
 */
@State(Scope.Benchmark)
class ParserBenchmark {
    private lateinit var largeDocument: String
    private lateinit var tokens: List<String>
    private lateinit var longPrefix: String

    /** Builds the inputs once per benchmark run. */
    @Setup
    fun setUp() {
        largeDocument = BenchmarkDocuments.document(LARGE_DOCUMENT_SECTIONS)
        tokens = BenchmarkDocuments.tokens(BenchmarkDocuments.document(STREAMED_DOCUMENT_SECTIONS), STREAMED_TOKENS)
        longPrefix = largeDocument + "\n\n"
    }

    /** One-shot parse of a document of about 73 KB. */
    @Benchmark
    fun parseLargeDocument(): MarkdownDocument = MarkdownParser().parse(largeDocument)

    /** 2,000 tokens appended one at a time, with a document produced after each, then finished. */
    @Benchmark
    fun streamTokensIncrementally(): MarkdownDocument {
        val parser = IncrementalMarkdownParser()
        for (token in tokens) parser.append(token)
        return parser.finish()
    }

    /**
     * The same 2,000 tokens appended after a 73 KB document: the cost per token should not depend on
     * how much text came before.
     */
    @Benchmark
    fun streamTokensAfterLongDocument(): MarkdownDocument {
        val parser = IncrementalMarkdownParser()
        parser.setText(longPrefix)
        for (token in tokens) parser.append(token)
        return parser.finish()
    }

    /** The same stream, re-parsing the whole text after each token: what a non-incremental renderer does. */
    @Benchmark
    fun streamTokensWithFullReparse(): MarkdownDocument {
        val parser = MarkdownParser()
        val text = StringBuilder()
        var document: MarkdownDocument? = null
        for (token in tokens) {
            text.append(token)
            document = parser.parse(text.toString())
        }
        return checkNotNull(document)
    }

    private companion object {
        const val LARGE_DOCUMENT_SECTIONS = 100
        const val STREAMED_DOCUMENT_SECTIONS = 12
        const val STREAMED_TOKENS = 2_000
    }
}

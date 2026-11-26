package io.github.springmusk026.composdown.benchmarks

/** Deterministic inputs that resemble language model output. */
internal object BenchmarkDocuments {
    private val section =
        """
        ## Section NUMBER

        This section explains **an important idea** with *emphasis*, `inline code`, a [link](https://example.com/NUMBER)
        and a ~~mistake~~. It continues over several lines so that the paragraph has realistic length,
        mentioning www.example.com and an address like team@example.com along the way.

        1. First step with `code`.
        2. Second step, which has more text in it to wrap.
           - Nested detail **NUMBER**.
           - [x] Done task
        3. Third step.

        ```kotlin
        fun section(number: Int): String {
            val label = "Section ${'$'}number"
            return label.uppercase()
        }
        ```

        | Name | Value | Notes |
        | :--- | ----: | ----- |
        | alpha | NUMBER | first *row* |
        | beta | 42 | second row |

        > A quotation with a footnote[^fNUMBER] and more text.

        [^fNUMBER]: Footnote NUMBER.

        """.trimIndent()

    /** A document of [sections] repeated sections, about 0.7 KB each. */
    fun document(sections: Int): String = (1..sections).joinToString("\n") { section.replace("NUMBER", it.toString()) }

    /**
     * Splits [text] into [count] tokens of a few characters each, the way a language model streams
     * text: words, whitespace runs and punctuation.
     */
    fun tokens(
        text: String,
        count: Int,
    ): List<String> {
        val tokens = Regex("""\s+|\w+|[^\w\s]""").findAll(text).map { it.value }.toList()
        require(tokens.size >= count) { "The text has only ${tokens.size} tokens" }
        return tokens.take(count)
    }
}

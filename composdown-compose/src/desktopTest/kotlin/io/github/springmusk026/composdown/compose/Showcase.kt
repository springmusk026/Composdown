package io.github.springmusk026.composdown.compose

/** A document with every kind of element, for screenshot tests. */
internal object Showcase {
    val MARKDOWN =
        """
        # Heading 1
        ## Heading 2
        ### Heading 3

        A paragraph with *emphasis*, **strong emphasis**, ~~strikethrough~~, `inline code`,
        a [link](https://example.com) and a footnote reference[^1].

        - First item
        - Second item
          - Nested item
            - Deeper item
        - [x] Completed task
        - [ ] Open task

        1. One
        2. Two

        > A block quote with **formatting**.

        > [!WARNING]
        > Alerts stand out from the text.

        ```kotlin
        fun greet(name: String) = "Hello, ${'$'}name!"
        ```

        | Left | Center | Right |
        | :--- | :----: | ----: |
        | a | b | c |
        | longer cell | `code` | 42 |

        ---

        [^1]: The footnote text.
        """.trimIndent()
}
